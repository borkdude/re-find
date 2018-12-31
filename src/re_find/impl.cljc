(ns ^:no-doc re-find.impl
  (:require
   [clojure.math.combinatorics :refer [permutations]]
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
       (let [ret-val# ~expr]
         ;; use pr-str to force lazy seq
         ;; example where this is needed: (match :args [#"a(.*)" "abc"] :ret seq)
         (binding [*print-length* 1]
           (pr-str ret-val#))
         ret-val#)
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
  (let [args-spec (:args spec)
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
                  (try! (apply (sym->fn sym) (second args))))
        ret-val-match (if (or ret-fn?
                              exact-ret-match?)
                        (cond
                          exact-ret-match?
                          (if (try! (= ret-expected ret-val))
                            ret-val
                            ::invalid)
                          ;; this evaluates to a truthy value
                          (try! (ret-expected ret-val)) ret-val
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

(defn match [sym+spec args printable-args ret opts]
  (if-not args
    [(match-1 sym+spec args ret opts)]
    (let [permutations? (:permutations? opts)
          args-permutations (if permutations? (permutations (second args)) [(second args)])
          printable-args-permutations (if permutations? (permutations printable-args) [printable-args])]
      (map #(match-1 sym+spec [:args %1] ret (assoc opts
                                                    :printable-args %2
                                                    :permutation? %3))
           args-permutations printable-args-permutations
           (cons false (repeat true))))))
