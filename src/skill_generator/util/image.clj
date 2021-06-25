(ns skill-generator.util.image
  (:require [clojure.java.io :as jio])
  (:import (java.awt.image BufferedImage)
           (java.awt Image Color)
           (java.nio.file Paths Files)
           (javax.imageio ImageIO)
           (java.io OutputStream IOException InputStream)
           (java.nio.file.attribute FileAttribute)))

(def passive-border-part (take 32 (cons 31 (iterate identity 255))))

(defn resize-to [w h ^BufferedImage origin]
  (doto (BufferedImage. w h BufferedImage/TYPE_INT_RGB)
    (-> (.getGraphics)
        (.drawImage (.getScaledInstance origin w h Image/SCALE_SMOOTH) 0 0 nil))
    )
  )

(def resize-to-64 (partial resize-to 64 64))

(defn write-blp-and-close! [^OutputStream out ^BufferedImage image]
  (ImageIO/write image "blp" out)
  (.close out)
  )

(defn copy [^BufferedImage image]
  (let [w (.getWidth image nil)
        h (.getHeight image nil)
        new-image (BufferedImage. w h BufferedImage/TYPE_INT_RGB)]
    (-> new-image
        (.createGraphics)
        (.drawImage image 0 0 nil)
        )
    new-image
    )
  )

(defn- do-add-border! [^BufferedImage image ^InputStream is]
  (let [border (ImageIO/read ^InputStream is)]
    (-> image
        (.getGraphics)
        (.drawImage border 0 0 nil))
    image)

  )

(defn add-active-border [origin & opts]
  (let [result (copy origin)]
    (if opts
      (case (:filter-type (first opts))
        :none result
        :default (do-add-border! result (try (jio/input-stream (jio/resource "active_border2.png"))
                                             (catch IOException e (throw (IllegalStateException. "未找到默认滤镜，请使用自定义滤镜")))))
        :self-defined (do-add-border! result (try (jio/input-stream (:filter-url (first opts)))
                                                  (catch IOException e (throw (IllegalStateException. "滤镜图片无效")))))
        )
      result
      )
    )
  )

(defn- mix-pixels [px0, p0, px1, p1]
  (let [r0 (bit-and (bit-shift-right px0 16) 0xFF)
        r1 (bit-and (bit-shift-right px1 16) 0xFF)
        g0 (bit-and (bit-shift-right px0 8) 0xFF)
        g1 (bit-and (bit-shift-right px1 8) 0xFF)
        b0 (bit-and px0 0xFF)
        b1 (bit-and px1 0xFF)
        r (int (+ (* r0 p0) (* r1 p1)))
        g (int (+ (* g0 p0) (* g1 p1)))
        b (int (+ (* b0 p0) (* b1 p1)))
        ]
    (bit-or (bit-shift-left (bit-shift-right px0 24) 24) (bit-shift-left r 16) (bit-shift-left g 8) b)
    )
  )

(defn- mix-pixels-for-border [^BufferedImage image ^BufferedImage border x y p]
  (.setRGB image x y (mix-pixels (.getRGB image x y)
                                 (/ p 255)
                                 (.getRGB border x y)
                                 (- 1 (/ p 255))))
  )

(defn add-passive-border [origin]
  (let [result (copy origin) border (ImageIO/read (jio/resource "passive_border.png"))]
    (loop [start 0 end 63]
      (if (< start end)
        (do (doseq [i (range start end)]
              (mix-pixels-for-border result border start i (nth passive-border-part start))
              (mix-pixels-for-border result border end i (nth passive-border-part start))
              (mix-pixels-for-border result border i start (nth passive-border-part start))
              (mix-pixels-for-border result border i end (nth passive-border-part start))
              )
            (recur (inc start) (dec end))
            )
        result
        ))
    )
  )

(defn make-valid [c]
  (cond (> c 255) 255
        (< c 0) 0
        :else c)
  )

(defn- make-new-color [^Color color brightness]
  (Color.
    ^int (make-valid (+ (.getRed color) brightness))
    ^int (make-valid (+ (.getGreen color) brightness))
    ^int (make-valid (+ (.getBlue color) brightness)))
  )

(defn adjust-brightness [^BufferedImage origin brightness]
  (let [result (copy origin)]
    (doseq [i (range 64) j (range 64)]
      (.setRGB result j i (-> result
                              (.getRGB j i)
                              (Color.)
                              (make-new-color brightness)
                              (.getRGB)
                              ))
      )
    result
    )
  )


(defn add-border [^BufferedImage origin type & opts]
  (case type
    :active (apply add-active-border origin opts)
    :passive (add-passive-border origin)
    )
  )

(defn output-as-blp [^BufferedImage image dir name prefix]
  (let [path (Paths/get dir (into-array String []))]
    (Files/createDirectories path (into-array FileAttribute []))
    (-> path
        (.resolve (str prefix name ".blp"))
        (.toFile)
        (jio/output-stream)
        (write-blp-and-close! image)
        ))
  )
