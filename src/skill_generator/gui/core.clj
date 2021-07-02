(ns skill-generator.gui.core
  (:use [seesaw [core :exclude (separator)] font border mig forms]
        [skill-generator.core :as core]
        )
  (:import (com.formdev.flatlaf FlatLightLaf)))


(defn frame-content []
  (let [project-dir "E:/IdeaProjects/JZJH/jzjh" get-id (fn [type] (core/get-one-available-id project-dir type))]
    (forms-panel
      "pref,4dlu,pref,4dlu,pref,4dlu,pref,4dlu,pref"
      :items [
              (separator "功能区")
              (action :name "查找可用技能ID" :handler (fn [e] (alert e (get-id "ability"))))
              (action :name "查找可用物品ID" :handler (fn [e] (alert e (get-id "item"))))
              (action :name "查找可用单位ID" :handler (fn [e] (alert e (get-id "unit"))))
              (action :name "查找可用BUFFID" :handler (fn [e] (alert e (get-id "buff"))))
              (action :name "查找可用英雄ID" :handler (fn [e] (alert e (get-id "hero"))))
              (action :name "查找可用ID" :handler (fn [e] (alert e "123")))
              ]
      )
    )
  )

(defn get-frame []
  (frame :title "决战江湖门派生成器"
         :resizable? false
         :content (frame-content)
         :on-close :exit
         )
  )


(defn -main [& args]
  (FlatLightLaf/setup)
  (invoke-later
    (-> (get-frame)
        pack!
        show!
        ))
  )
