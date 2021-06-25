(ns skill-generator.util.template
  (:require [selmer.parser :as par]
            [skill-generator.util.pinyin :as pinyin]
            [clojure.string :as str]))

(defn split-combination [comb]
  (let [v (clojure.string/split comb #"：")]
   (zipmap [:name :val] (map-indexed #(if (= 0 %) (str %2 "：") %2) v))
    )
  )

(defn get-icon-path [name type]
  (par/render "ReplaceableTextures\\\\CommandButtons\\\\{{prefix}}{{name}}.blp"
              {:prefix (if (= type "passive") "PASBTN" "BTN")
               :name (pinyin/get-pinyin-name (str name "."))})
  )

(defn convert-to-data [abilities type]
  (let [typed-abilities (filterv #(= type (:type %)) abilities)]
    (map
      #(assoc %
         :icon-path (get-icon-path (:name %) type)
         :items (range 1 (if (:level %) (inc (:level %)) 10))
         :combinations (map split-combination (:combinations %)))
      typed-abilities)
    )
  )

(defn render [abilities type]
  (let [data (convert-to-data abilities type) file (if (= type "passive") "passive.ini" "active.ini") ]
    (str/join "\n" (map #(par/render-file file %) data))
    )
  )


(defn -main [& args]
  (println (split-combination "+弹指神通：伤害+80%"))
  ;(println (par/render-file "passive.ini" {:id "A017", :items (range 1 10)}))
  )
