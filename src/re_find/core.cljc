(ns re-find.core
  (:require
   #?(:clj [clojure.spec.alpha :as s]
      :cljs [cljs.spec.alpha :as s])
   [clojure.spec.test.alpha :as stest]
   [re-find.finitize :refer [finitize]]
   [re-find.impl :as impl]))

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
  (let [finitize? (:finitize? opts)
        args (find opts :args)
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
        specs (map s/get-spec syms)
        sym*spec (zipmap syms specs)]
    (filter identity (mapcat #(impl/match % args ret opts) sym*spec))))

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
