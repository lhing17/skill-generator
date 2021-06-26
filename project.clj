(defproject skill-generator "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url  "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 ;; https://mvnrepository.com/artifact/org.clojure/data.json
                 [org.clojure/data.json "2.3.1"]
                 [com.belerweb/pinyin4j "2.5.1"]
                 [com.github.PhoenixZeng/BLP_IIO_Plugins "f3cfe38a66"]
                 [selmer "1.12.40"]
                 [commons-io "2.6"]
                 ]
  :repl-options {:init-ns skill-generator.core}
  :repositories [["jitpack" "https://jitpack.io"]]
  )
