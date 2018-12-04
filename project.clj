(defproject re-find "0.0.1-SNAPSHOT"
  :description "find functions by matching specs"
  :url "https://github.com/borkdude/re-find"
  :scm {:name "git"
        :url "https://github.com/borkdude/re-find"}
  :license {:name "MIT"
            :url "https://mit-license.org/"}
  :dependencies [[org.clojure/clojure "1.10.0-RC3"]
                 [org.clojure/tools.cli "0.4.1"]]
  :profiles {:dev {:dependencies
                   [[org.clojure/clojurescript "1.10.439"]
                    [org.clojure/test.check "0.9.0"]
                    [speculative "0.0.3-SNAPSHOT"]]}}
  :deploy-repositories [["clojars" {:url "https://clojars.org/repo"
                                    :username :env/clojars_username
                                    :password :env/clojars_password
                                    :sign-releases false}]])
