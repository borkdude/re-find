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
        ret-expected (when ret
                       (let [v (second ret)
                             v (if finitize? (finitize v)
                                   v)
                             v (if (and (:sequential? opts)
                                        (sequential? v))
                                 #(= v %)
                                 v)]
                         v))
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
                            true
                            ::invalid)
                          ret-fn? (let [r (try! (ret-expected ret-val))]
                                    ;; this should evaluate to true and
                                    ;; not ::invalid
                                    (if (and r (not= ::invalid r))
                                      true
                                      ::invalid))
                          :else ::invalid)
                        true)]
    (when (and args-match?
               ret-spec-match?
               (not= ::invalid ret-val)
               (not= ::invalid ret-val-match))
      (merge opts {:sym sym
                   :args-spec args-spec
                   :ret-spec ret-spec}
             (when args {:ret-val ret-val})
             (when (or (nil? ret) ret-fn? (= ret-expected ret-val))
               {:exact? true})))))

(defn permutations* [{:keys [:args :printable-args]}]
  (let [args* (rest (permutations args))
        printable-args* (rest (permutations printable-args))]
    (map (fn [a pa]
           {:args a
            :printable-args pa
            :permutation? true})
         args*
         printable-args*)))

(defn splice-last-arg [{:keys [:args :printable-args] :as m}]
  (let [l (last args)
        lp (last printable-args)]
    (if (and (some? l) (sequential? l))
      (let [printable-literal?
            (cond (list? lp) ;; compare to evaluated version
                  (= lp l)
                  :else (= (type lp) (type l)))
            new-args (into (vec (butlast args)) l)
            ;; we use the evaluated last arg if it's not a literal, because we
            ;; do not want to display list 1 2 3 instead of 1 2 3 when the
            ;; printable arg is (list 1 2 3)
            new-printable-args (into (vec (butlast printable-args))
                                     (if printable-literal? lp l))]
        [{:args new-args
          :printable-args new-printable-args}])
      [])))

(defn match [sym+spec args ret opts]
  (if-not args
    [(match-1 sym+spec args ret opts)]
    (let [args (if (:finitize? opts)
                 (mapv finitize (second args))
                 (second args))
          printable-args (or (:printable-args opts)
                             (mapv pr-str args))
          args-and-printable {:args args :printable-args printable-args}
          permutations? (:permutations? opts)
          splice-last-arg? (:splice-last-arg? opts)
          ;; add permutations
          permutated (when permutations?
                       (permutations* args-and-printable))
          ;; add spliced
          spliced (when splice-last-arg?
                    (mapcat splice-last-arg (cons args-and-printable permutated)))]
      (map #(match-1 sym+spec [:args (:args %)] ret
                     (assoc opts
                            :printable-args (:printable-args %)
                            :permutation? (:permutation? %)))
           (cons args-and-printable (concat permutated spliced))))))
