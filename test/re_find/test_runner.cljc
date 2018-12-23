(ns re-find.test-runner
  (:require
   [clojure.test :as t]
   [re-find.core-test]))

(defn -main [& args]
  (t/run-tests 're-find.core-test))

#?(:cljs (set! *main-cli-fn* -main))
