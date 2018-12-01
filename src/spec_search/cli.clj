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

(defn try-apply [f args]
  {:ret-val (try (apply f args)
                 (catch Exception _ ::invalid))})

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
              [:ret (read-ret v)])
        exact-ret-match? (:exact-ret-match options)
        _ (assert (if exact-ret-match? ret true)
                  "--exact-ret-match needs --ret")
        search-opts (cond-> {:exact-ret-match? exact-ret-match?}
                      args (assoc :args args)
                      ret (assoc :ret (second ret)))
        search-results (search/search search-opts)
        search-results
        (if (and args print-ret-vals)
          (map (fn [sr]
                 (if (:ret-val sr)
                   sr
                   (merge sr
                          (try-apply (resolve (:sym sr))
                                     args))))
               search-results)
          ;; TODO: we probably have to handle ::invalid here
          search-results)]
    (if (and args print-ret-vals)
      (pprint/print-table
       ["function" "arguments" "return value"]
       (keep (fn [{:keys [ret-val] :as m}]
               (when ret-val
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

  )
