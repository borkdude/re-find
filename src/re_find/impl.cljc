(ns ^:no-doc re-find.impl
  (:require
   [clojure.math.combinatorics :refer [permutations]]
   [re-find.finitize :refer [finitize]]
   #?(:clj [clojure.spec.alpha :as s]
      :cljs [cljs.spec.alpha :as s])
   #?(:cljs [goog.object :as gobject]))
  #?(:cljs (:require-macros
            [re-find.impl :refer [? try!]])))

(defmacro deftime
  "Private. deftime macro from https://github.com/cgrand/macrovich"
  [& body]
  (when #?(:clj (not (:ns &env))
           :cljs (when-let [n (and *ns* (ns-name *ns*))]
                   (re-matches #".*\$macros" (name n))))
    `(do ~@body)))

(deftime

  (defmacro ?
    "Private. case macro from https://github.com/cgrand/macrovich"
    [& {:keys [cljs clj]}]
    (if (contains? &env '&env)
      `(if (:ns ~'&env) ~cljs ~clj)
      (if #?(:clj (:ns &env) :cljs true)
        cljs
        clj)))

  (defmacro try! [expr]
    `(try
       ~expr
       (catch ~(? :clj 'Exception :cljs ':default) _#
         ::invalid))))

(defn sym->fn [sym]
  #?(:cljs
     ;; TODO, use (goog.getObjectByName (munge "cljs.core.some?"))
     (let [ns (munge (namespace sym))
           nm (munge (name sym))]
       (gobject/get (js/eval ns) nm))
     :clj (resolve sym)))

(defn match-1
  [[sym spec] args ret opts]
  (let [finitize? (:finitize? opts)
        args-spec (:args spec)
        ret-spec (:ret spec)
        exact-ret-match? (:exact-ret-match? opts)
        ret-expected (second ret)
        ret-fn? (fn? ret-expected)
        args-match? (or (not args)
                        (and args args-spec
                             (let [res (try! (s/valid? args-spec
                                                       (second args)))]
                               (when (not= ::invalid res)
                                 res))))
        ret-spec-match? (or (not ret)
                            ret-fn?
                            (let [res (try! (s/valid? ret-spec ret-expected))]
                              (when (not= ::invalid res)
                                res)))
        ret-val (when (and (not (:safe? opts))
                           args
                           args-match?)
                  (try! (let [r (apply (sym->fn sym) (second args))]
                          (if finitize? (finitize r) r))))
        ret-val-match (if (or ret-fn?
                              exact-ret-match?)
                        (cond
                          exact-ret-match?
                          (if (try! (= ret-expected ret-val))
                            ret-val
                            ::invalid)
                          ret-fn? (let [r (try! (ret-expected ret-val))]
                                    ;; this should evaluate to true and
                                    ;; not ::invalid
                                    (if (and r (not= ::invalid r))
                                      {}
                                      ::invalid))
                          :else ::invalid)
                        {})]
    (when (and args-match?
               ret-spec-match?
               (not= ::invalid ret-val)
               (not= ::invalid ret-val-match))
      (merge opts {:sym sym
                   :args-spec args-spec
                   :ret-spec ret-spec}
             (when args {:ret-val ret-val})))))

(defn match [sym+spec args ret opts]
  (if-not args
    [(match-1 sym+spec args ret opts)]
    (let [args [:args (if (:finitize? opts)
                        (mapv finitize (second args))
                        (second args))]
          printable-args (or (:printable-args opts)
                             (mapv pr-str (second args)))
          permutations? (:permutations? opts)
          args-permutations (if permutations? (permutations (second args)) [(second args)])
          printable-args-permutations (if permutations? (permutations printable-args) [printable-args])]
      (map #(match-1 sym+spec [:args %1] ret (assoc opts
                                            :printable-args %2
                                            :permutation? %3))
           args-permutations printable-args-permutations
           (cons false (repeat true))))))
