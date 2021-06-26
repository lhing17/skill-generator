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

(defn- get-all-current-ids [project-dir]
  (let [ability-path "table/ability.ini"]
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
  (not-any? #(= id %) current-ids)
  )

(defn get-available-ids [n current-ids]
  "获取n个可用ID"
  (loop [ids [], count n, id "A000"]
    (cond (= count 0) ids
          (available? id current-ids) (recur (conj ids id) (dec count) (next-id id))
          :else (recur ids count (next-id id))
          )
    )
  )

(defn- add-id-for-abilities [ids abilities]
  "将ID添加到技能的配置中"
  (map #(assoc %2 :id %) ids abilities)
  )


(defn get-abilities [project-dir base-out-dir]
  (let [current-ids (get-all-current-ids project-dir)
        abilities (load-config (str base-out-dir "/config.json"))
        count (count abilities)
        available-ids (get-available-ids count current-ids)]
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
  (map (comp #(File. ^String %) #(str base-out-dir "/" % ".png") :name)
       (filter #(= type (:type %)) abilities))
  )

(defn generate-icons [base-out-dir abilities opts]
  (doseq [type ["active" "passive"] f (get-png-files base-out-dir abilities type)]
    (convert-to-blp! f type base-out-dir opts))
  )

(defn -main [& args]
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
    (spit path (str (slurp path) "\n" passive active))

    )
  )
