(ns spec-search.core
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.test.alpha :as stest]))

(defn try-apply [sym args]
  (try {:ret-val (apply (resolve sym) args)}
       ;; force result here?
       (catch Exception _
         ::invalid)))

(defn search*
  [[sym spec] args ret opts]
  (let [args-spec (:args spec)
        ret-spec (:ret spec)
        exact-ret-match? (:exact-ret-match? opts)
        ret-expected (second ret)
        ret-fn? (fn? ret-expected)
        args-match? (or (not args)
                        (and args args-spec
                             (s/valid? args-spec
                                       (second args))))
        ret-spec-match? (or (not ret)
                            ret-fn?
                            (and ret ret-spec
                                 (s/valid? ret-spec ret-expected)))
        ret-val (when-not (:safe? opts)
                  (try-apply sym (second args)))
        ret-val-match (if (or ret-fn?
                              exact-ret-match?)
                        (cond
                          exact-ret-match?
                          (if (try (= ret-expected (:ret-val ret-val))
                                   (catch Exception _ ::invalid))
                            ret-val
                            ::invalid)
                          (ret-expected (:ret-val ret-val)) ret-val
                          :else ::invalid)
                        {})]
    (when (and args-match?
               ret-spec-match?
               (not= ::invalid ret-val)
               (not= ::invalid ret-val-match))
      (cond-> (merge {:sym sym} ret-val)
        args (assoc :args (second args))))))

(defn search
  "Search spec that matches args and/or ret opt.
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
    (keep #(search* % args ret opts) sym*spec)))

;;;; Scratch

(comment

  (require '[speculative.core])
  (search :args [inc [1 2 3]] :ret [2 3 4])
  (search :args [inc [1 2 3]] :ret [2 3 4] :exact-ret-match? true)

  (require '[clojure.pprint :as pprint])
  (pprint/print-table
   (map #(update % :ret-val pr-str)
        (search :args [inc [1 2 3]] :ret [2 3 4])))

  (search :args [] :ret nil :exact-ret-match? true) ;; merge
  (search :exact-ret-match? true :args []) ;; exception
  (search :safe? true :exact-ret-match? true :ret nil :args []) ;; exception
  (search :args [8] :ret number?)
  (search :args [8] :ret number? :safe? true) ;; exception

  )
