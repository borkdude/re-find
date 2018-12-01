(ns spec-search.cli
  (:require
   [clojure.edn :as edn]
   [clojure.pprint :as pprint]
   [clojure.tools.cli :refer [parse-opts]]
   [spec-search.core :as search]))

(defn try-resolve [s]
  (if (and (symbol? s)
           (not (re-find #"^['`]" (str s))))
    (resolve s)
    s))

(defn read-args [s]
  (when s
    (mapv try-resolve
          (edn/read-string (format "[%s]" s)))))

(defn read-ret [s]
  (when s
    (try-resolve
     (edn/read-string s))))

(defn try-apply [f args]
  (try (pr-str (apply f args))
       (catch Exception _ nil)))

(def cli-options
  [["-a" "--args ARGUMENTS" "Arguments"]
   ["-r" "--ret RETVAL" "Return value"]
   ["-e" "--exact-ret-match" "Return value must match on value"]
   ["-v" "--print-ret-vals" "Filter and print on succesful return values"]
   ["-h" "--help"]])

(defn -main [& args]
  (let [options (:options (parse-opts args cli-options))
        args (when-let [[_ v] (find options :args)]
               (read-args v))
        print-ret-vals (:print-ret-vals options)
        _ (assert (if print-ret-vals args true)
                  "--print-ret-vals needs --args")
        ret (when-let [[_ v] (find options :ret)]
              (read-ret v))
        exact-ret-match? (:exact-ret-match options)
        _ (assert (if exact-ret-match? ret true)
                  "--exact-ret-match needs --ret")
        search-opts (cond-> {:exact-ret-match? exact-ret-match?}
                      args (assoc :args args)
                      ret (assoc :ret ret))
        syms (search/search search-opts)
        fns (map resolve syms)
        rets (when (and args print-ret-vals)
               (map #(try-apply % args) fns))
        syms+rets (zipmap syms rets)]
    (if (seq rets)
      (pprint/print-table
       ["function" "arguments" "return value"]
       (keep (fn [[k v]]
               (when v
                 {"function" k
                  "arguments" (:args options)
                  "return value" v})) syms+rets))
      (pprint/pprint syms))))

(comment

  (-main "--args" "inc [1 2 3]" "--ret" "[]" "-e") ;; remove
  (-main "--args" "inc [1 2 3]" "--ret" "[2 3 4]" "-e") ;; map

  )
