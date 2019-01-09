(ns re-find.cli
  (:require
   [clojure.pprint :as pprint]
   [clojure.tools.cli :refer [parse-opts]]
   [re-find.core :refer [match]]
   [clojure.set :as set]))

(defn read-args [s]
  (eval (read-string (format "[%s]" s))))

(defn read-ret [s]
  (eval (read-string s)))

(def cli-options
  [["-a" "--args ARGUMENTS" "arguments"]
   ["-r" "--ret RETVAL" "return value"]
   ["-e" "--exact-ret-match" "return value must match on value"]
   ["-s" "--safe" "safe: no evaluation of functions on given arguments"]
   ["-v" "--verbose" "prints table with return values"]
   ["-p" "--permutations" "try with permutations on args"]
   ["-f" "--finitize" "prevent evaluation of infinite collections"]])

(defn -main [& args]
  (let [options (:options (parse-opts args cli-options))
        args (when-let [[_ v] (find options :args)]
               (read-args v))
        ret (when-let [[_ v] (find options :ret)]
              [:ret (read-ret v)])
        exact-ret-match? (:exact-ret-match options)
        safe? (:safe options)
        permutations? (:permutations options)
        finitize? (:finitize options)
        search-opts (cond-> {:exact-ret-match? exact-ret-match?
                             :safe? safe?}
                      args (assoc :args args)
                      ret (assoc :ret (second ret))
                      permutations? (assoc :permutations? permutations?)
                      finitize? (assoc :finitize? finitize?))
        search-results (apply match (mapcat identity search-opts))]
    (if (:verbose options)
      (pprint/print-table
       ["function" "arguments" "return value"]
       (keep (fn [{:keys [ret-val] :as m}]
               (when (not= ::invalid ret-val)
                 (-> m
                     (assoc :args (:args options))
                     (update :ret-val #(binding [*print-length* 10]
                                         (pr-str %)))
                     (set/rename-keys
                      {:sym "function"
                       :args "arguments"
                       :ret-val "return value"}))))
             search-results))
      (pprint/pprint (map :sym search-results)))))

(comment

  (require '[speculative.core.extra])
  (require '[speculative.instrument])
  (-main "--args" "inc [1 2 3]" "--ret" "[]" "-e") ;; remove
  (-main "--args" "inc [1 2 3]" "--ret" "[2 3 4]" "-e" "-v") ;; map
  (-main "--args" "nil" "--ret" "nil" "-e") ;; first, merge
  (-main "--args" "inc [1 2 3]" "--ret" "[2 3 4]" "-e" "-v")
  (-main "--args" "8" "--ret" "4" "-v")
  (-main "--args" "8" "--ret" "number?" "-v")
  (-main "--args" "#{1 2} #{2 3}" "--ret" "set?" "-v")
  (-main "--args" "#{1 2} #{2 3}" "--ret" "set?")
  (-main "--args" "nil" "--ret" "nil" "-e")
  (-main "--args" "8" "--ret" "number?" "--safe") ;; exception

  )
