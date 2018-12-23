(ns re-find.core-test
  (:require
   [clojure.test :as t :refer [deftest is are]]
   [re-find.core :as re-find :refer [match]]
   [speculative.instrument]
   [re-find.impl :refer [?]]
   [clojure.set :as set]))

(defn matches? [match-results match-result]
  (let [results (set/project match-results (keys match-result))]
    (contains? results match-result)))

(defn core-sym [name]
  #?(:clj (symbol "clojure.core" name)
     :cljs (symbol "cljs.core" name)))

(deftest match-test
  (is (matches? (match :args [inc [1 2 3]] :ret [2 3 4] :exact-ret-match? true)
                {:sym (core-sym "map")}))
  (let [results (match :args [8] :ret 4)]
    (are [x] (matches? results x)
      {:sym (core-sym "/")}
      {:sym (core-sym "some?")}))
  (let [results (match :args [8] :ret number?)]
    (are [pred x] (pred (matches? results x))
      true? {:sym (core-sym "/")}
      false? {:sym (core-sym "some?")}))
  (let [results (match :args [#{1 2} #{3 4}] :ret set?)]
    (are [x] (matches? results x)
      {:sym 'clojure.set/intersection}
      {:sym 'clojure.set/difference}
      {:sym 'clojure.set/union}
      {:sym 'clojure.set/select}))
  (let [results (match :args [nil] :ret nil :exact-ret-match? true)]
    (are [x] (matches? results x)
      {:sym (core-sym "first")}
      {:sym (core-sym "merge")}
      {:sym 'clojure.set/intersection}
      {:sym 'clojure.set/difference}
      {:sym 'clojure.set/union}))
  (let [results (match :args [#"b" "abc"] :ret "b" :exact-ret-match? true)]
    (are [x] (matches? results x)
      {:sym (core-sym "re-find")})))

(deftest assert-test
  (is (thrown-with-msg? #?(:clj AssertionError :cljs :default)
                        #"exact-ret-match\? true but no ret passed"
                        (match :exact-ret-match? true :args [])))
  (is (thrown-with-msg? #?(:clj AssertionError :cljs :default)
                        #"exact-ret-match\? is true or ret is fn\? but safe\? is set to true"
                        (match :safe? true :exact-ret-match? true :ret nil :args [])))
  (is (thrown-with-msg? #?(:clj AssertionError :cljs :default)
                        #"exact-ret-match\? is true or ret is fn\? but safe\? is set to true"
                        (match :args [8] :ret number? :safe? true))))

;;;; Scratch

(comment
  (t/run-tests)
  )
