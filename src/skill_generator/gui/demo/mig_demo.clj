(ns skill-generator.gui.demo.mig-demo
  (:use [seesaw core font border mig]
        )
  (:import (com.formdev.flatlaf FlatLightLaf)))

(defn frame-content []
  (mig-panel :constraints ["", "[right]"]
             :items [
                     [ "General"          "split, span, gaptop 10"]
                     [ :separator         "growx, wrap, gaptop 10"]
                     [ "Company"          "gap 10"]
                     [ (text)             "span, growx"]
                     [ "Contact"          "gap 10"]
                     [ (text)             "span, growx, wrap"]

                     [ "Propeller"        "split, span, gaptop 10"]
                     [ :separator         "growx, wrap, gaptop 10"]

                     [ "PTI/kW"           "gap 10"]
                     [ (text :columns 10) ""]
                     [ "Power/kW"         "gap 10"]
                     [ (text :columns 10) "wrap"]
                     [ "R/mm"             "gap 10"]
                     [ (text :columns 10) ""]
                     [ "D/mm"             "gap 10"]
                     [ (text :columns 10) ""]]))
(defn open-more-options-dlg
  []
  (let [ok-act     (action :name "Ok" :handler (fn [e] (return-from-dialog e "OK")))
        cancel-act (action :name "Cancel" :handler (fn [e] (return-from-dialog e "Cancel")))]
    (-> (custom-dialog
          :modal? true
          :title "More Options"
          :content (flow-panel :items [ok-act cancel-act]))
        pack!
        show!)))

(defn open-display-options-dlg
  []
  (let [ok-act (action
                 :name "Ok"
                 :handler (fn [e] (return-from-dialog e (value (to-frame e)))))
        cancel-act (action :name "Cancel"
                           :handler (fn [e] (return-from-dialog e nil)))
        more-act (action :name "More ..."
                         :handler (fn [e] (alert (str "More Result = " (open-more-options-dlg)))))]
    (-> (custom-dialog
          :title  "Display Options"
          :modal? true
          :resizable? false
          :content (mig-panel
                     :border (line-border)
                     :items [[(label :font (font :from (default-font "Label.font") :style :bold)
                                     :text "Display options for new geometry")
                              "gaptop 10, wrap"]

                             [:separator "growx, wrap, gaptop 10, spanx 2"]

                             ["Display mode:"]

                             [(combobox :id :mode
                                        :model ["Triangulated Mesh" "Lines"])
                              "wrap"]

                             ["Angle"]

                             [(slider :id :angle
                                      :min 0 :max 20
                                      :minor-tick-spacing 1 :major-tick-spacing 20
                                      :paint-labels? true)
                              "wrap"]

                             [(flow-panel :align :right :items [more-act ok-act cancel-act])
                              "spanx 2" "alignx right"]]))
        pack!
        show!)))

(defn frame-content2 []
  (action :name "Show Dialog"
          :handler (fn [e]
                     (alert (str "Result = " (open-display-options-dlg)))))
  )

(defn get-frame []
  (frame :title "jGoodies FormLayout Example"
         :resizable? false
         :content (frame-content)))


(defn -main [& args]
  (FlatLightLaf/setup)
  (invoke-later
    (-> (get-frame)
        pack!
        show!
        ))
  )

