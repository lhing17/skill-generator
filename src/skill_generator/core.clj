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

(def opts {:filter-type :default})
(def base-out-dir "F:/War3Map/generate_icons")
(def project-dir "E:/IdeaProjects/JZJH/jzjh")
(def command-dir (str base-out-dir "/ReplaceableTextures/CommandButtons"))
(def command-disabled-dir (str base-out-dir "/ReplaceableTextures/CommandButtonsDisabled"))

(defn load-config [path]
  (json/read-str (slurp path) :key-fn keyword)
  )

(defn get-all-current-ids [path]
  (let [ability-path "table/ability.ini"]
    (with-open [rdr (jio/reader (str path "/" ability-path))]
      (doall (map #(str/replace % #"\[|\]" "")
                  (filter
                    #(and (str/starts-with? % "[") (str/ends-with? % "]"))
                    (line-seq rdr))))
      ))
  )

(defn next-char [c]
  "计算下一个字符：\\9的下一个字符为\\A，\\Z的下一个字符为\\0"
  (let [i (int c)]
    (cond (and (>= i 48) (<= i 56)) (char (inc i))
          (= i 57) \A
          (and (>= i 65) (<= i 89)) (char (inc i))
          (= i 90) \0
          )
    )
  )

(defn inc-by-index [s index]
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
  (loop [ids [], count n, id "A000"]
    (cond (= count 0) ids
          (available? id current-ids) (recur (conj ids id) (dec count) (next-id id))
          :else (recur ids count (next-id id))
          )
    )
  )

(defn add-id-for-abilities [ids abilities]
  (map #(assoc %2 :id %) ids abilities)
  )


(defn get-abilities []
  (let [current-ids (get-all-current-ids project-dir)
        abilities (load-config (str base-out-dir "/config.json"))
        count (count abilities)
        available-ids (get-available-ids count current-ids)]
    (add-id-for-abilities available-ids abilities))
  )






(defn- output-blp [image name adjust-image type dir prefix]
  (let [border (img/add-border (adjust-image image) type opts)]
    (img/output-as-blp border dir (pinyin/get-pinyin-name name) prefix)
    image
    )
  )

(defn- output-active-blp [image name]
  (output-blp image name identity :active command-dir "BTN")
  )

(defn- output-active-dark-blp [image name]
  (output-blp image name #(img/adjust-brightness % -50) :passive command-disabled-dir "DISBTN")
  )

(defn- output-passive-blp [image name]
  (output-blp image name identity :passive command-dir "PASBTN"))

(defn- output-passive-dark-blp [image name]
  (output-blp image name #(img/adjust-brightness % -64) :passive command-disabled-dir "DISPASBTN"))

(defn- output-blp-fn [type]
  (case type
    :active output-active-blp
    :active-dark output-active-dark-blp
    :passive output-passive-blp
    :passive-dark output-passive-dark-blp)
  )

(defn convert-to-blp! [^File file type]
  (-> file
      (ImageIO/read)
      (img/resize-to-64)
      ((output-blp-fn (keyword type)) (.getName file))
      ((output-blp-fn (keyword (str type "-dark"))) (.getName file))
      )
  )

(defn get-png-files [path abilities type]
  (map (comp #(File. ^String %) #(str path "/" % ".png") :name)
       (filter #(= type (:type %)) abilities))
  )

(defn generate-icons [path abilities]
  (doseq [type ["active" "passive"] f (get-png-files path abilities type)]
    (convert-to-blp! f type))
  )

(defn -main [& args]
  (let [abilities (get-abilities)
        passive (tpl/render abilities "passive")
        active (tpl/render abilities "active")
        path (str project-dir "/table/ability.ini")
        ]
    (generate-icons base-out-dir abilities)
    (FileUtils/copyDirectoryToDirectory
      (File. (str base-out-dir "/ReplaceableTextures"))
      (File. (str project-dir "/resource")))
    (spit path (str (slurp path) "\n" passive active))

    )
  )
