(ns re-find.core
  (:require #?(:clj [clojure.spec.alpha :as s]
               :cljs [cljs.spec.alpha :as s])
            [clojure.spec.test.alpha :as stest]
            #?(:cljs [goog.object :as gobject]))
  #?(:cljs (:require-macros
            [re-find.core :refer [try!]])))

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

(defn- match-1
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
        ret-val (when-not (:safe? opts)
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
      (cond-> (merge {:sym sym} {:ret-val ret-val})
        args (assoc :args (second args))))))

(defn match
  "Find functions by specs that match args and/or ret opt.
   Argument opts can have:
    :args - value to search matching args spec
    :ret - value to search matching ret spec. if passed a fn? then it
     will replace the ret spec. If :exact-ret-match? is also true, then
     return value must exactly match the function.
    :safe? - forbid evaluation of found function with args. useful for side effecting functions
    :exact-ret-match? - if true, return value must be equal to :ret
   At minimum args or ret must be specified."
  [& {:as opts}]
  (let [args (find opts :args)
        ret (find opts :ret)
        _ (assert (or args ret) "At minimum provide args or ret")
        safe? (:safe? opts)
        _ (when (:exact-ret-match? opts)
            (assert ret "exact-ret-match? true but no ret passed"))
        eval? (or (:exact-ret-match? opts)
                  (fn? (second ret)))
        _ (assert (if eval? (not safe?) true)
                  "exact-ret-match? is true or ret is fn? but safe? is set to true")
        _ (assert (if eval? args true)
                  "exact-ret-match? is true or ret is fn? but no args are given")
        syms (stest/instrumentable-syms)
        specs (map
               s/get-spec (stest/instrumentable-syms)) sym*spec (zipmap syms specs)]
    (keep #(match-1 % args ret opts) sym*spec)))

;;;; Scratch

(comment

  (require '[speculative.instrument])
  (match :args [inc [1 2 3]] :ret [2 3 4])
  (match :args [inc [1 2 3]] :ret [2 3 4] :exact-ret-match? true)

  (require '[clojure.pprint :as pprint])
  (pprint/print-table
   (map #(update % :ret-val pr-str)
        (match :args [inc [1 2 3]] :ret [2 3 4])))

  (match :args [] :ret nil :exact-ret-match? true) ;; merge
  (match :exact-ret-match? true :args []) ;; exception
  (match :safe? true :exact-ret-match? true :ret nil :args []) ;; exception

  (require '[speculative.core.extra])
  (match :args [8] :ret number?)
  (match :args [8] :ret number? :safe? true) ;; exception

  (match :args [#"b" "abc"] :ret "b" :exact-ret-match? true)

  )
