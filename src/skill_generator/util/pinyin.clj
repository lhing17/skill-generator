(ns skill-generator.util.pinyin
  (:require [clojure.string :as str])
  (:import (net.sourceforge.pinyin4j PinyinHelper)
           (net.sourceforge.pinyin4j.format HanyuPinyinVCharType HanyuPinyinToneType HanyuPinyinOutputFormat)
           (net.sourceforge.pinyin4j.format.exception BadHanyuPinyinOutputFormatCombination)))


(defn get-pinyin-name [name]
  (let [o-name (subs name 0 (str/index-of name "."))]
    (try (PinyinHelper/toHanYuPinyinString o-name
                                           (doto (HanyuPinyinOutputFormat.)
                                             (.setVCharType HanyuPinyinVCharType/WITH_V)
                                             (.setToneType HanyuPinyinToneType/WITHOUT_TONE))
                                           ""
                                           true)
         (catch BadHanyuPinyinOutputFormatCombination _ o-name))
    )
  )
