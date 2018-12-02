(ns spec-search.cli
  (:require
   [clojure.edn :as edn]
   [clojure.pprint :as pprint]
   [clojure.tools.cli :refer [parse-opts]]
   [spec-search.core :as search]
   [clojure.set :as set]))

(defn try-resolve [s]
  (if (and (symbol? s)
           (not (re-find #"^['`]" (str s))))
    (deref (resolve s))
    s))

(defn read-args [s]
  (when s
    (mapv try-resolve
          (edn/read-string (format "[%s]" s)))))

(defn read-ret [s]
  (when s
    (try-resolve
     (edn/read-string s))))

(def cli-options
  [["-a" "--args ARGUMENTS" "arguments"]
   ["-r" "--ret RETVAL" "return value"]
   ["-e" "--exact-ret-match" "return value must match on value"]
   ["-s" "--safe" "safe: no eval will happen on arguments"]
   ["-v" "--verbose" "prints table with return values"]])

(defn -main [& args]
  (let [options (:options (parse-opts args cli-options))
        args (when-let [[_ v] (find options :args)]
               (read-args v))
        ret-vals (find options :ret-vals)
        ret (when-let [[_ v] (find options :ret)]
              [:ret (read-ret v)])
        exact-ret-match? (:exact-ret-match options)
        no-eval? (:safe options)
        search-opts (cond-> {:exact-ret-match? exact-ret-match?
                             :no-eval? no-eval?}
                      args (assoc :args args)
                      ret (assoc :ret (second ret))
                      ret-vals (assoc :ret-vals? (second ret-vals)))
        search-results (apply search/search (mapcat identity search-opts))]
    (if (:verbose options)
      (pprint/print-table
       ["function" "arguments" "return value"]
       (keep (fn [{:keys [ret-val] :as m}]
               (when (not= ::invalid ret-val)
                 (-> m
                     (assoc :args (:args options))
                     (update :ret-val pr-str)
                     (set/rename-keys
                      {:sym "function"
                       :args "arguments"
                       :ret-val "return value"}))))
             search-results))
      (pprint/pprint (map :sym search-results)))))

(comment

  (-main "--args" "inc [1 2 3]" "--ret" "[]" "-e") ;; remove
  (-main "--args" "inc [1 2 3]" "--ret" "[2 3 4]" "-e") ;; map
  (-main "--args" "nil" "--ret" "nil" "-e") ;; first, merge

  (-main "--args" "inc [1 2 3]" "--ret" "[2 3 4]" "-e" "-v")
  (require '[speculative.core.extra])
  (require '[speculative.instrument])
  (-main "--args" "8" "--ret" "4" "-v")
  (-main "--args" "8" "--ret" "number?" "-v")
  (-main "--args" "#{1 2} #{2 3}" "--ret" "set?" "-v")
  (-main "--args" "#{1 2} #{2 3}" "--ret" "set?")
  (-main "--args" "nil" "--ret" "nil" "-e")
  (-main "--args" "8" "--ret" "number?" "--safe") ;; exception

  )
