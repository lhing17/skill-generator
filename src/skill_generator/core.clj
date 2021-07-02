(ns skill-generator.core
  (:require [clojure.data.json :as json]
            [clojure.java.io :as jio]
            [clojure.string :as str]
            [skill-generator.util.image :as img]
            [skill-generator.util.template :as tpl]
            [skill-generator.util.pinyin :as pinyin]
            )
  (:import (javax.imageio ImageIO)
           (java.io File)
           (org.apache.commons.io FileUtils)))


(defn- command-dir [base-out-dir] (str base-out-dir "/ReplaceableTextures/CommandButtons"))
(defn- command-disabled-dir [base-out-dir] (str base-out-dir "/ReplaceableTextures/CommandButtonsDisabled"))

(defn load-config [config-path]
  (json/read-str (slurp config-path) :key-fn keyword)
  )

(defn get-all-current-ids [project-dir type]
  "获取某类型当前所有的ID"
  (let [ability-path (str "table/" (if (= type "hero") "unit" type) ".ini")]
    (with-open [rdr (jio/reader (str project-dir "/" ability-path))]
      (doall (map #(str/replace % #"\[|\]" "")
                  (filter
                    #(and (str/starts-with? % "[") (str/ends-with? % "]"))
                    (line-seq rdr))))
      ))
  )

(defn- next-char [c]
  "计算下一个字符：\\9的下一个字符为\\A，\\Z的下一个字符为\\0"
  (let [i (int c)]
    (cond (and (>= i 48) (<= i 56)) (char (inc i))
          (= i 57) \A
          (and (>= i 65) (<= i 89)) (char (inc i))
          (= i 90) \0
          (and (>= i 97) (<= i 121)) (char (inc i))
          (= i 122) \0
          )
    )
  )

(defn- inc-by-index [s index]
  "对下标为index的字符取next-char"
  (apply str (concat (take index s)
                     (cons (next-char (nth s index))
                           (nthrest s (inc index))
                           )))
  )

(defn next-id [id]
  "计算下一个ID"
  (loop [index (dec (count id)), cid (inc-by-index id index)]
    (if (= \0 (nth cid index))
      (recur (dec index) (inc-by-index cid (dec index)))
      cid
      )
    )
  )

(defn- available? [id current-ids]
  (not-any? #(= (str/lower-case id) (str/lower-case %)) current-ids)
  )

(defn get-available-ids [n current-ids type]
  "获取n个可用ID"
  (let [start-id
        (cond (= type "ability") "A000"
              (= type "item") "I000"
              (= type "unit") "e000"
              (= type "hero") "E000"
              (= type "buff") "B000"
              :else "A000")]
    (loop [ids [], count n, id start-id]
      (cond (= count 0) ids
            (available? id current-ids) (recur (conj ids id) (dec count) (next-id id))
            :else (recur ids count (next-id id))
            )
      ))
  )

(defn- add-id-for-abilities [ids abilities]
  "将ID添加到技能的配置中"
  (map #(assoc %2 :id %) ids abilities)
  )


(defn get-abilities [project-dir base-out-dir]
  "获取到要添加技能的Json数据，含ID"
  (let [current-ids (get-all-current-ids project-dir "ability")
        abilities (load-config (str base-out-dir "/config.json"))
        count (count abilities)
        available-ids (get-available-ids count current-ids "ability")]
    (add-id-for-abilities available-ids abilities))
  )

(defn- output-blp [adjust-image type dir-fn prefix]
  (fn [image name base-out-dir opts]
    (let [border (img/add-border (adjust-image image) type opts)]
      (img/output-as-blp border (dir-fn base-out-dir) (pinyin/get-pinyin-name name) prefix)
      image
      )
    )
  )

(defn- output-blp-fn [type]
  (case type
    :active (output-blp identity :active command-dir "BTN")
    :active-dark (output-blp #(img/adjust-brightness % -50) :passive command-disabled-dir "DISBTN")
    :passive (output-blp identity :passive command-dir "PASBTN")
    :passive-dark (output-blp #(img/adjust-brightness % -64) :passive command-disabled-dir "DISPASBTN"))
  )

(defn convert-to-blp! [^File file type base-out-dir opts]
  (-> file
      (ImageIO/read)
      (img/resize-to-64)
      ((output-blp-fn (keyword type)) (.getName file) base-out-dir opts)
      ((output-blp-fn (keyword (str type "-dark"))) (.getName file) base-out-dir opts)
      )
  )

(defn get-png-files [base-out-dir abilities type]
  "获取到技能的png图片"
  (map (comp #(File. ^String %) #(str base-out-dir "/" % ".png") :name)
       (filter #(= type (:type %)) abilities))
  )

(defn generate-icons [base-out-dir abilities opts]
  "根据技能生成图标"
  (doseq [type ["active" "passive"] f (get-png-files base-out-dir abilities type)]
    (convert-to-blp! f type base-out-dir opts))
  )

(defn append-to-file [path content & more]
  "向文件末尾添加内容"
  (spit path (apply str (slurp path) "\n" content more))
  )

(defn generate-abilities []
  "生成技能"
  (let [base-out-dir "F:/git_repos/skill-generator/examples"
        project-dir "F:/git_repos/JZJH/jzjh"
        abilities (get-abilities project-dir base-out-dir)
        passive (tpl/render abilities "passive")
        active (tpl/render abilities "active")
        path (str project-dir "/table/ability.ini")
        opts {:filter-type :default}
        ]
    (generate-icons base-out-dir abilities opts)
    (FileUtils/copyDirectoryToDirectory
      (File. (str base-out-dir "/ReplaceableTextures"))
      (File. (str project-dir "/resource")))
    (append-to-file path passive active)
    )
  )

(defn ^String get-one-available-id [project-dir type]
  (first (get-available-ids 1 (get-all-current-ids project-dir type) type))
  )

(defn- render-denom [name ancestor project-dir ability-names]
  (tpl/render "denom.ini" {:id        (get-one-available-id project-dir "item")
                           :icon-path (tpl/get-icon-path name "active")
                           :store     false
                           :name      name
                           :ancestor  ancestor
                           :kungfu1   (str/join " " (take 3 ability-names))
                           :kungfu2   (str/join " " (take-last 2 ability-names))
                           })

  )

(defn render-dummy-immolation [project-dir & [{:keys [range damage name]}]]
  "永久的献祭马甲技能"
  (tpl/render "immolation.ini" {
                                :id     (get-one-available-id project-dir "ability")
                                :range  (if (nil? range) 200 range)
                                :damage damage
                                :name   (if (nil? name) "马甲技能" name)
                                })
  )

(defn render-dummy-unit [project-dir & [{:keys [name model ability_id]}]]
  "马甲单位"
  (tpl/render "dummy.ini" {
                           :id         (get-one-available-id project-dir "unit")
                           :name       (if (nil? name) "技能马甲" name)
                           :model      (if (nil? model) ".mdl" model)
                           :ability_id ability_id
                           })
  )

(defn do-generate-denom [name path ancestor project-dir base-out-dir opts ability-names]
  (let [item-ini (str project-dir "/table/item.ini")]
    (convert-to-blp! (File. (str path "/" name ".png")) "active" base-out-dir opts)
    (append-to-file item-ini (render-denom name ancestor project-dir ability-names))
    )
  )

(defn generate-denom []
  (let [base-out-dir "F:/git_repos/skill-generator/examples"
        project-dir "F:/git_repos/JZJH/jzjh"
        opts {:filter-type :default}
        abilities (get-abilities project-dir base-out-dir)
        ]
    (do-generate-denom "雪山派" base-out-dir "白自在" project-dir base-out-dir opts (map :name abilities))
    )
  )

(defn render-exclusive [project-dir & [{:keys [name description]}]]
  "专属"
  (tpl/render "exclusive.ini" {
                               :id          (get-one-available-id project-dir "item")
                               :name        name
                               :icon-path   (tpl/get-icon-path name "passive")
                               :description description
                               })
  )

(defn- exclusive-description [denom bonuses three-attr six-attr extra]
  (str "|cff33ff00〓" denom "专属神器〓|r|n"
       "|cff00ffff类别:武器|r|n"
       "|cffcc9999等级:|r|cff33ff00S|r|n"
       "|cffffff33" (str/join "|n" bonuses) "|n|r"
       "|cff99cc00" (str/join "|n" three-attr) "|n|r"
       "|cffff6600" (str/join "|n" six-attr) "|n|r"
       "|cff33ff00" extra "|r"
       )
  )

(defn do-generate-exclusive [name base-out-dir project-dir {:keys [denom bonuses three-attr six-attr extra]}]
  (let [description (exclusive-description denom bonuses three-attr six-attr extra)
        exclusive-content (render-exclusive project-dir {:name name :description description})
        item-ini (str project-dir "/table/item.ini")]
    (convert-to-blp! (File. (str base-out-dir "/" name ".jpg")) "passive" base-out-dir {:filter-type :default})
    (append-to-file item-ini exclusive-content)
    (FileUtils/copyDirectoryToDirectory
      (File. (str base-out-dir "/ReplaceableTextures"))
      (File. (str project-dir "/resource")))
    )
  ;|cffffff33攻击+5000|n攻击速度+60%|n暴击伤害+150%|n绝学领悟力+5|n杀怪回复+6000|n|r|cff99cc00招式伤害+300|n内力+300|r|cffffff33|n|r|cffff6600胆魄+5|n经脉+4|r|n|cff33ff00加强泰山派技能伤害，增加泰山十八盘概率|r

  )

(defn generate-exclusive [project-dir base-out-dir]
  (do-generate-exclusive "碧水剑" base-out-dir project-dir {
                                                         :denom      "雪山派"
                                                         :bonuses    ["攻击速度+60%" "暴击伤害+150%" "绝学领悟力+5" "杀怪回复+6000"]
                                                         :three-attr ["招式伤害+300" "真实伤害+300"]
                                                         :six-attr   ["医术+5" "福缘+4"]
                                                         :extra      "加强雪山派技能伤害"
                                                         })
  )

;(defn -main [& args]
;  (let [project-dir "E:/IdeaProjects/JZJH/jzjh"
;        base-out-dir "F:/War3Map/generate_icons"
;        unit-ini (str project-dir "/table/unit.ini")
;        ;unit-content (render-dummy-unit project-dir {:name "风沙莽莽马甲" :ability_id "A0D3" :model "war3mapImported\\\\sandbreathdamage.mdl"})
;        ]
;    ;(generate-exclusive project-dir base-out-dir)
;    ;(append-to-file unit-ini unit-content)
;    (println (get-one-available-id project-dir "unit"))
;    )
;
;  )
