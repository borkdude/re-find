(ns re-find.finitize)

(defprotocol Finitize
  (finitize [x]))

(def take-max 100)
(defn default-finitize [x]
  (doall (take take-max x)))

(extend-protocol Finitize

  #?(:clj Object :cljs default)
  (finitize [x] x)

  #?(:clj clojure.lang.LazySeq :cljs LazySeq)
  (finitize [x] (default-finitize x))

  #?(:clj clojure.lang.Cons :cljs Cons)
  (finitize [x] (default-finitize x))

  #?(:clj clojure.lang.Range :cljs Range)
  (finitize [x] (default-finitize x))

  #?(:clj clojure.lang.Iterate :cljs Iterate)
  (finitize [x] (default-finitize x))

  #?(:clj clojure.lang.Repeat :cljs Repeat)
  (finitize [x] (default-finitize x)))
