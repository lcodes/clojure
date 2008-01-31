;   Copyright (c) Rich Hickey. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Common Public License 1.0 (http://opensource.org/licenses/cpl.php)
;   which can be found in the file CPL.TXT at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(in-ns 'clojure)

(def list (. clojure.lang.PersistentList creator))

(def cons (fn [x seq] (. clojure.lang.RT (cons x seq))))
(def conj (fn [coll x] (. clojure.lang.RT (conj coll x))))

(def defn (fn [name & fdecl]
              (list 'def name (cons 'fn fdecl))))

(. (the-var defn) (setMacro))

(defn instance? [#^Class c x]
  (. c (isInstance x)))
  
(defn vector
      ([] [])
      ([& args]
          (. clojure.lang.PersistentVector (create args))))

(defn hash-map
      ([] {})
      ([& args]
          (. clojure.lang.PersistentHashMap (create args))))

(defn sorted-map
      ([& args]
          (. clojure.lang.PersistentTreeMap (create args))))

(defn sorted-map-by
      ([comparator & args]
          (. clojure.lang.PersistentTreeMap (create comparator args))))

;;;;;;;;;;;;;;;;; metadata ;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn meta [#^clojure.lang.IObj x]
 (. x (meta)))

(defn with-meta [#^clojure.lang.IObj x m]
  (. x (withMeta m)))


  
;;;;;;;;;;;;;;;;;;;;
(def defmacro (fn [name & args]
                  (list 'do
                        (cons 'defn (cons name args))
                        (list '. (list 'the-var name) '(setMacro)))))

(. (the-var defmacro) (setMacro))

(defmacro when [test & body]
   (list 'if test (cons 'do body)))

(defmacro when-not [test & body]
   (list 'if test nil (cons 'do body)))

(defn #^Boolean nil? [x] (identical? x nil))
(defn #^Boolean false? [x] (identical? x false))
(defn #^Boolean true? [x] (identical? x true))

(defn not [x] (if x false true))

(defn first [x] (. clojure.lang.RT (first x)))

(defn rest [x] (. clojure.lang.RT (rest x)))

(defn second [x] (. clojure.lang.RT (second x)))

(defn ffirst [x] (first (first x)))
(defn rfirst [x] (rest (first x)))
(defn frest [x] (first (rest x)))
(defn rrest [x] (rest (rest x)))

(defn #^Boolean = [x y] (. clojure.lang.RT (equal x y)))
(defn #^Boolean not= [x y] (not (= x y)))

(defn #^String str [#^Object x]
  (if x (. x (toString)) ""))

(defn #^String strcat 
  ([] "")
  ([x] (str x))
  ([x & ys]
    (loop [sb (new StringBuilder (str x)) more ys]
      (if more
          (recur (. sb  (append (str (first more)))) (rest more))
        (str sb)))))

(defn symbol
  ([name] (. clojure.lang.Symbol (intern name)))
  ([ns name] (. clojure.lang.Symbol (intern ns name))))

(defn keyword
  ([name] (. clojure.lang.Keyword (intern nil name)))
  ([ns name] (. clojure.lang.Keyword (intern ns name))))

(defn gensym 
  ([] (gensym "G__"))
  ([prefix-string] (. clojure.lang.Symbol (intern (strcat prefix-string (str (. clojure.lang.RT (nextID))))))))

(defmacro cond [& clauses]
  (when clauses
    (list 'if (first clauses)
            (second clauses)
            (cons 'cond (rest (rest clauses))))))

(defn seq [coll]
  (. clojure.lang.RT (seq coll)))

(defn spread [arglist]
      (cond
       (nil? arglist) nil
       (nil? (rest arglist)) (seq (first arglist))
       :else (cons (first arglist) (spread (rest arglist)))))

(defn apply [#^clojure.lang.IFn f & args]
      (. f (applyTo (spread args))))

(defn list* [arg & args]
      (spread (cons arg args)))

(defmacro delay [& body]
  (list 'new 'clojure.lang.Delay (list* 'fn [] body)))

(defn fnseq [x restfn]
  (new clojure.lang.FnSeq x restfn))

(defmacro lazy-cons [x & body]
  (list 'fnseq x (list* 'fn [] body)))


  
(defn concat
      ([] nil)
      ([x & xs]
          (cond
           (nil? xs) (seq x)
           (nil? (seq x)) (recur (first xs) (rest xs))
           :else (lazy-cons (first x) (apply concat (rest x) xs)))))

;;at this point all the support for syntax-quote exists

(defmacro and
  ([] true)
  ([x] x)
  ([x & rest]
    `(let [and# ~x]
       (if and# (and ~@rest) and#))))

(defmacro or
  ([] nil)
  ([x] x)
  ([x & rest]
      `(let [or# ~x]
         (if or# or# (or ~@rest)))))

;;;;;;;;;;;;;;;;;;; sequence fns  ;;;;;;;;;;;;;;;;;;;;;;;

(defn reduce
  ([f coll]
     (if (seq coll)
       (reduce f (first coll) (rest coll))
      (f)))
  ([f val coll]
    (if (seq coll)
       (recur f (f val (first coll)) (rest coll))
      val)))

(defn reverse [coll]
  (reduce conj nil coll))
  
;;math stuff
(defn +
      ([] 0)
      ([x] x)
      ([x y] (. clojure.lang.Num (add x y)))
      ([x y & more]
          (reduce + (+ x y) more)))

(defn *
      ([] 1)
      ([x] x)
      ([x y] (. clojure.lang.Num (multiply x y)))
      ([x y & more]
          (reduce * (* x y) more)))

(defn /
      ([x] (/ 1 x))
      ([x y] (. clojure.lang.Num (divide x y)))
      ([x y & more]
          (reduce / (/ x y) more)))

(defn -
      ([x] (. clojure.lang.Num (negate x)))
      ([x y] (. clojure.lang.Num (subtract x y)))
      ([x y & more]
          (reduce - (- x y) more)))

(defn <
      ([x] true)
      ([x y] (. clojure.lang.Num (lt x y)))
      ([x y & more]
          (when (< x y)
            (if (rest more)
                (recur y (first more) (rest more))
                (< y (first more))))))

(defn <=
      ([x] true)
      ([x y] (. clojure.lang.Num (lte x y)))
      ([x y & more]
          (when (<= x y)
            (if (rest more)
                (recur y (first more) (rest more))
                (<= y (first more))))))

(defn >
      ([x] true)
      ([x y] (. clojure.lang.Num (gt x y)))
      ([x y & more]
          (when (> x y)
            (if (rest more)
                (recur y (first more) (rest more))
                (> y (first more))))))

(defn >=
      ([x] true)
      ([x y] (. clojure.lang.Num (gte x y)))
      ([x y & more]
          (when (>= x y)
            (if (rest more)
                (recur y (first more) (rest more))
                (>= y (first more))))))

(defn ==
      ([x] true)
      ([x y] (. clojure.lang.Num (equiv x y)))
      ([x y & more]
          (when (== x y)
            (if (rest more)
                (recur y (first more) (rest more))
                (== y (first more))))))

(defn max
  ([x] x)
  ([x y] (if (> x y) x y))
  ([x y & more]
   (reduce max (max x y) more)))

(defn min
  ([x] x)
  ([x y] (if (< x y) x y))
  ([x y & more]
   (reduce min (min x y) more)))

(defn inc [x]
      (. clojure.lang.Num (inc x)))

(defn dec [x]
      (. clojure.lang.Num (dec x)))

(defn #^Boolean pos? [x]
      (. clojure.lang.Num (posPred x)))

(defn #^Boolean neg? [x]
      (. clojure.lang.Num (negPred x)))

(defn #^Boolean zero? [x]
      (. clojure.lang.Num (zeroPred x)))

(defn quot [num div]
  (. clojure.lang.Num (quotient num div)))

(defn rem [num div]
  (. clojure.lang.Num (remainder num div)))

;;Bit ops

(defn bit-shift-left [x y]
  (. clojure.lang.IntegerNum (shiftLeft x y)))

(defn bit-shift-right [x y]
  (. clojure.lang.IntegerNum (shiftRight x y)))

(defn bit-and [x y]
  (. clojure.lang.IntegerNum (bitAnd x y)))

(defn bit-or [x y]
  (. clojure.lang.IntegerNum (bitOr x y)))

(defn bit-xor [x y]
  (. clojure.lang.IntegerNum (bitXor x y)))

(defn bit-not [x]
  (. clojure.lang.IntegerNum (bitNot x)))

(defn complement [f]
  (fn [& args]
    (not (apply f args))))

(defn constantly [x]
  (fn [& args] x))

(defn identity [x] x)

;;Collection stuff



(defn count [coll]
  (. clojure.lang.RT (count coll)))

;;list stuff
(defn peek [list]
  (. clojure.lang.RT (peek list)))

(defn pop [list]
  (. clojure.lang.RT (pop list)))

(defn nth [coll index]
 (. clojure.lang.RT (nth coll index)))

;;map stuff

(defn contains? [map key]
 (. clojure.lang.RT (contains map key)))

(defn get
  ([map key]
    (. clojure.lang.RT (get map key)))
  ([map key not-found]
    (. clojure.lang.RT (get map key not-found))))

(defn assoc [map key val]
 (. clojure.lang.RT (assoc map key val)))

(defn dissoc [map key]
 (. clojure.lang.RT (dissoc map key)))

(defn find [map key]
 (. clojure.lang.RT (find map key)))

(defn select [map keyseq]
 (loop [ret {} keys (seq keyseq)]
   (if keys
        (let [entry (. clojure.lang.RT (find map (first keys)))]
            (recur
                (if entry
                    (conj ret entry)
                   ret)
                (rest keys)))
      ret)))

(defn keys [map]
  (. clojure.lang.RT (keys map)))

(defn vals [map]
  (. clojure.lang.RT (vals map)))

(defn key [#^java.util.Map$Entry e]
 (. e (getKey)))

(defn val [#^java.util.Map$Entry e]
 (. e (getValue)))

(defn rseq [#^clojure.lang.Reversible rev]
  (. rev (rseq)))

(defn name [#^clojure.lang.Named x]
  (. x (getName)))

(defn namespace [#^clojure.lang.Named x]
  (. x (getNamespace)))

(defn andfn [& args]
      (if (nil? (rest args))
          (first args)
        (and (first args) (recur (rest args)))))

(defn orfn [& args]
      (if (nil? args)
          nil
        (or (first args) (recur (rest args)))))


(defmacro locking [x & body]
  `(let [lockee# ~x]
        (try
           (monitor-enter lockee#)
           ~@body
           (finally
             (monitor-exit lockee#)))))

(defmacro ..
  ([x form] `(. ~x ~form))
  ([x form & more] `(.. (. ~x ~form) ~@more)))

(defmacro ->
  ([x form] `(~(first form) ~x ~@(rest form)))
  ([x form & more] `(-> (-> ~x ~form) ~@more)))

;;multimethods
(defmacro defmulti
  ([name dispatch-fn] (defmulti name dispatch-fn :default))
  ([name dispatch-fn default-val]
    `(def ~name (new clojure.lang.MultiFn ~dispatch-fn ~default-val))))

(defmacro defmethod [multifn dispatch-val & fn-tail]
  `(let [pvar# (the-var ~multifn)]
      (. pvar# (commuteRoot (fn [mf#] (. mf# (assoc ~dispatch-val (fn ~@fn-tail))))))))

(defmacro remove-method [multifn dispatch-val]
  `(let [pvar# (the-var ~multifn)]
      (. pvar# (commuteRoot (fn [mf#] (. mf# (dissoc ~dispatch-val)))))))

;;;;;;;;; var stuff      

(defmacro binding [bindings & body]
  (let [var-ize (fn [var-vals]
                    (loop [ret [] vvs (seq var-vals)]
                          (if vvs
                              (recur  (conj (conj ret `(the-var ~(first vvs))) (second vvs))
                                      (rest (rest vvs)))
                            (seq ret))))]
    `(try
      (. clojure.lang.Var (pushThreadBindings (hash-map ~@(var-ize bindings))))
      ~@body
      (finally
        (. clojure.lang.Var (popThreadBindings))))))

(defn find-var [sym]
 (. clojure.lang.Var (find sym)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; Refs ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn agent [state]
 (new clojure.lang.Agent state))

(defn agent-of [state]
 (:agent ^state))

(defn ! [#^clojure.lang.Agent a f & args]
  (. a (dispatch f args)))

(defn agent-errors [#^clojure.lang.Agent a]
  (. a (getErrors)))

(defn clear-agent-errors [#^clojure.lang.Agent a]
  (. a (clearErrors)))

(defn ref [x]
 (new clojure.lang.Ref x))

(defn deref [#^clojure.lang.IRef ref]
  (. ref (get)))

(defn commute [#^clojure.lang.Ref ref fun & args]
  (. ref (commute fun args)))

(defn alter [#^clojure.lang.Ref ref fun & args]
  (. ref (alter fun args)))

(defn set [#^clojure.lang.Ref ref val]
    (. ref (set val)))

(defn ensure [#^clojure.lang.Ref ref]
    (. ref (touch))
    (. ref (get)))

(defmacro sync [flags-ignored-for-now & body]
  `(. clojure.lang.LockingTransaction
    (runInTransaction (fn [] ~@body))))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; fn stuff ;;;;;;;;;;;;;;;;


(defn comp [& fs]
  (let [fs (reverse fs)]
     (fn [& args]
       (loop [ret (apply (first fs) args) fs (rest fs)]
          (if fs
              (recur ((first fs) ret) (rest fs))
             ret)))))

(defn partial
	([f arg1]
	   (fn [& args] (apply f arg1 args)))
	([f arg1 arg2]
	   (fn [& args] (apply f arg1 arg2 args)))
	([f arg1 arg2 arg3]
	   (fn [& args] (apply f arg1 arg2 arg3 args)))
	([f arg1 arg2 arg3 & more]
	  (fn [& args] (apply f arg1 arg2 arg3 (concat more args)))))

;;;;;;;;;;;;;;;;;;; sequence fns  ;;;;;;;;;;;;;;;;;;;;;;;


  
(defn #^Boolean every? [pred coll]
  (if (seq coll)
     (and (pred (first coll))
          (recur pred (rest coll)))
    true))

(def #^Boolean not-every? (comp not every?))

(defn some [pred coll]
  (when (seq coll)
    (or (pred (first coll)) (recur pred (rest coll)))))

(def #^Boolean not-any? (comp not some))

(defn map
  ([f coll]
    (when (seq coll)
       (lazy-cons (f (first coll)) (map f (rest coll)))))
  ([f coll & colls]
    (when (and (seq coll) (every? seq colls))
      (lazy-cons (apply f (first coll) (map first colls))
                 (apply map f (rest coll) (map rest colls))))))

(defn mapcat [f & colls]
   (apply concat (apply map f colls)))

(defn filter [pred coll]
  (when (seq coll)
     (if (pred (first coll))
         (lazy-cons (first coll) (filter pred (rest coll)))
       (recur pred (rest coll)))))

(defn take [n coll]
  (when (and (pos? n) (seq coll))
    (lazy-cons (first coll) (take (dec n) (rest coll)))))

(defn take-while [pred coll]
  (when (and (seq coll) (pred (first coll)))
     (lazy-cons (first coll) (take-while pred (rest coll)))))

(defn drop [n coll]
  (if (and (pos? n) (seq coll))
      (recur (dec n) (rest coll))
     coll))

(defn drop-while [pred coll]
  (if (and (seq coll) (pred (first coll)))
      (recur pred (rest coll))
     coll))

(defn cycle [coll]
  (let [rep (fn [xs ys]
                (if xs
                  (lazy-cons (first xs) (rep (rest xs) ys))
                  (recur ys ys)))]
    (when (seq coll)
      (rep (seq coll) (seq coll)))))

(defn split-at [n coll]
  [(take n coll) (drop n coll)])

(defn split-with [pred coll]
  [(take-while pred coll) (drop-while pred coll)])

(defn repeat [x]
  (lazy-cons x (repeat x)))

(defn replicate [n x]
  (take n (repeat x)))
  
(defn iterate [f x]
   (lazy-cons x (iterate f (f x))))

(defn range
 ([end] (take end (iterate inc 0)))
 ([start end] (take (- end start) (iterate inc start)))
 ([start end step]
   (take-while (partial (if (pos? step) > <) end) (iterate (partial + step) start))))

(defn merge [& maps]
  (reduce conj maps))

(defn merge-with [f & maps]
  (let [merge-entry (fn [m e]
			(let [k (key e) v (val e)]
			  (if (contains? m k)
			    (assoc m k (f (m k) v))
			    (assoc m k v))))
	merge2 (fn [m1 m2]
		   (reduce merge-entry m1 (seq m2)))]
    (reduce merge2 maps)))



(defn zipmap [keys vals]
  (loop [map {}
         ks (seq keys)
         vs (seq vals)]
    (if (and ks vs)
        (recur (assoc map (first ks) (first vs))
               (rest ks)
               (rest vs))
       map)))

(defn line-seq [#^java.io.BufferedReader rdr]
  (let [line  (. rdr (readLine))]
    (when line
      (lazy-cons line (line-seq rdr)))))

(defn comparator [pred]
  (fn [x y] (cond (pred x y) -1 (pred y x) 1 :else 0)))
  
(defn sort
  ([#^java.util.Collection coll]
   (when (and coll (not (. coll (isEmpty))))
     (let [a (. coll (toArray))]
       (. java.util.Arrays (sort a))
       (seq a))))
  ([#^java.util.Comparator comp #^java.util.Collection coll]
   (when (and coll (not (. coll (isEmpty))))
     (let [a (. coll (toArray))]
       (. java.util.Arrays (sort a comp))
       (seq a)))))

(defn sort-by
  ([keyfn coll]
    (sort (fn [x y] (. #^Comparable (keyfn x) (compareTo (keyfn y)))) coll))
  ([keyfn #^java.util.Comparator comp coll]
    (sort (fn [x y] (. comp (compare (keyfn x) (keyfn y)))) coll)))

;; evaluation

(defn eval [form]
  (. clojure.lang.Compiler (eval form)))

(defn defimports [& imports-maps]
  (def *imports* (apply merge imports-maps)))

(defmacro doseq [item list & body]
  `(loop [list# (seq ~list)]
     (when list#
       (let [~item (first list#)]
         ~@body)
       (recur (rest list#)))))

(defn scan
  ([coll]
    (when (seq coll)
      (recur (rest coll))))
  ([n coll]
    (when (and (seq coll) (pos? n))
      (recur (dec n) (rest coll)))))

(defn touch
  ([coll]
   (scan coll)
   coll)
  ([n coll]
   (scan n coll)
   coll))

(defn await [& agents]
  (let [latch (new java.util.concurrent.CountDownLatch (count agents))
	count-down (fn [agent] (. latch (countDown)) agent)]
    (doseq agent agents
      (! agent count-down))
    (. latch (await))))

(defn await-for [timeout-ms & agents]
  (let [latch (new java.util.concurrent.CountDownLatch (count agents))
	count-down (fn [agent] (. latch (countDown)) agent)]
    (doseq agent agents
      (! agent count-down))
    (. latch (await  timeout-ms (. java.util.concurrent.TimeUnit MILLISECONDS)))))
  
(defmacro dotimes [i n & body]
  `(loop [~i 0 n# ~n]
     (when (< ~i n#)
       ~@body
       (recur (inc ~i) n#))))

(defn import [& import-lists]
 (when import-lists
   (let [#^clojure.lang.Namespace ns *ns*
         pkg (ffirst import-lists)
         classes (rfirst import-lists)]
       (doseq c classes
         (. ns (importClass c (. Class (forName (strcat pkg "." c)))))) )
   (apply import (rest import-lists))))

;(defn unimport [& names]
;   (let [#^clojure.lang.Var imps *ns-imports*]
;	  (doseq name names
;        (. imps (bindRoot (dissoc (. imps (get)) name))))))

;(defn refer [& refer-lists]
;  (doseq rlist refer-lists
;   (let [#^clojure.lang.Var refers *ns-refers*
;         ns (first rlist)
;         names (rest rlist)]
;     (doseq name names
;       (when (. clojure.lang.Var (find (symbol(str *ns*) (str name))))
;         (throw (new Exception (strcat "Name conflict: " name " already exists in this namespace"))))
;       (let [varsym (symbol (str ns) (str name))
;             var (. clojure.lang.Var (find varsym))
;             #^clojure.lang.Var rvar ((. refers (get)) name)]
;         (if var
;             (if rvar
;                 (when (not (= rvar var))
;                   (throw (new Exception (strcat "Name conflict: " name " already exists in this refer map as: " (. rvar sym)))))
;               (. refers (bindRoot (assoc (. refers (get)) name var))))
;            (throw (new Exception (strcat "Can't find Var: " varsym)))))))))

;(defn unrefer [& names]
;   (let [#^clojure.lang.Var refers *ns-refers*]
;	  (doseq name names
;        (. refers (bindRoot (dissoc (. refers (get)) name))))))

;(defn unintern [varsym]
;  (. clojure.lang.Var (unintern varsym)))

(defn into-array [aseq]
  (. clojure.lang.RT (seqToTypedArray (seq aseq))))

(defn into [to from]
  (let [ret to items (seq from)]
    (if items
       (recur (conj ret (first items)) (rest items))
      ret)))

(defn array [& items]
  (into-array items))

(defn make-proxy [classes method-map]
  (. java.lang.reflect.Proxy
    (newProxyInstance (. (identity clojure.lang.Compiler) (getClassLoader))
                      (into-array classes)
                      (new clojure.lang.ProxyHandler method-map))))

(defmacro implement [classes & fs]
  `(make-proxy
      ~classes
      ~(loop [fmap {} fs fs]
              (if fs
                  (recur (assoc fmap (name (ffirst fs))
                                     (cons 'fn (rfirst fs)))
                         (rest fs))
                 fmap))))

(defn pr
  ([] nil)
  ([x]
   (. clojure.lang.RT (print x *out*))
   nil)
  ([x & more]
   (pr x)
   (. *out* (append \space))
   (apply pr more)))

(defn newline []
  (. *out* (append \newline))
  nil)

(defn prn [& more]
  (apply pr more)
  (newline))

(defn print [& more]
  (binding [*print-readably* nil]
    (apply pr more)))

(defn println [& more]
  (binding [*print-readably* nil]
    (apply prn more)))

(defn read
  ([]
    (read *in*))
  ([stream]
    (read stream true nil))
  ([stream eof-error? eof-value]
    (read stream eof-error? eof-value false))
  ([stream eof-error? eof-value recursive?]
    (. clojure.lang.LispReader (read stream eof-error? eof-value recursive?))))

(defmacro with-open [rdr init & body]
  `(let [~rdr ~init]
     (try
      ~@body
      (finally
        (. ~rdr (close))))))

(defmacro doto [x & members]
  (let [gx (gensym)]
    `(let [~gx ~x]
       (do
         ~@(map (fn [m] (list '. gx m))
                members))
       ~gx)))

(defmacro memfn [name & args]
  `(fn [target# ~@args]
      (. target# (~name ~@args))))

(defmacro time [expr]
   `(let [start# (. System (nanoTime))
          ret# ~expr]
       (prn (strcat "Elapsed time: " (/ (- (. System (nanoTime)) start#) 1000000.0) " msecs"))
       ret#))


(defn #^Integer int [x]
  (. clojure.lang.RT (intCast x)))

(defn #^Long long [#^Number x]
  (. x (longValue)))

(defn #^Float float [#^Number x]
  (. x (floatValue)))

(defn #^Double double [#^Number x]
  (. x (doubleValue)))

(defn #^Short short [#^Number x]
  (. x (shortValue)))

(defn #^Byte byte [#^Number x]
  (. x (byteValue)))

(defn #^Character char [x]
  (. clojure.lang.RT (charCast x)))

(defn #^Boolean boolean [x]
  (if x true false))

(import '(java.lang.reflect Array))

(defn alength [array]
  (. Array (getLength array)))

(defn aget 
  ([array idx]
   (. Array (get array idx)))
  ([array idx & idxs]
   (apply aget (aget array idx) idxs)))

(defn aset
  ([array idx val]
   (. Array (set array idx val))
   val)
  ([array idx idx2 & idxv]
   (apply aset (aget array idx) idx2 idxv)))

(defmacro def-aset [name method coerce]
  `(defn ~name
    ([array# idx# val#]
     (. Array (~method array# idx# (~coerce val#)))
     val#)
    ([array# idx# idx2# & idxv#]
     (apply ~name (aget array# idx#) idx2# idxv#))))

(def-aset aset-int setInt int)
(def-aset aset-long setLong long)
(def-aset aset-boolean setBoolean boolean)
(def-aset aset-float setFloat float)
(def-aset aset-double setDouble double)
(def-aset aset-short setShort short)
(def-aset aset-byte setByte byte)
(def-aset aset-char setChar char)

(defn make-array 
  ([#^Class type len]
    (. Array (newInstance type (int len))))
  ([#^Class type dim & more-dims]
    (let [dims (cons dim more-dims)
          #^"[I" dimarray (make-array (. Integer TYPE)  (count dims))]
      (dotimes i (alength dimarray)
        (aset-int dimarray i (nth dims i)))
      (. Array (newInstance type dimarray)))))

(defn to-array [#^java.util.Collection coll]
  (. coll (toArray)))

(defn to-array-2d [#^java.util.Collection coll]
  (let [ret (make-array (. Class (forName "[Ljava.lang.Object;")) (. coll (size)))]
    (loop [i 0 xs (seq coll)]
      (when xs
        (aset ret i (to-array (first xs)))
        (recur (inc i) (rest xs))))
    ret))

(import '(java.util.concurrent Executors LinkedBlockingQueue))

(defn pmap
  ([f coll]
   (let [nthreads (.. Runtime (getRuntime) (availableProcessors))
         exec (. Executors (newFixedThreadPool nthreads))
         todo (ref (seq coll))
         out (ref 0)
         q (new LinkedBlockingQueue)
         produce (fn []
                     (let [job (sync nil
                                 (when @todo
                                   (let [item (first @todo)]
                                     (alter todo rest)
                                     (commute out inc)
                                     (list item))))]
                       (when job
                         (. q (put (f (first job))))
                         (recur))))
         tasks (doseq dnu (map (fn [task]
                                   (. exec (submit #^java.util.concurrent.Callable task)))
                               (replicate nthreads produce)))
         consume (fn []
                     (if (sync nil (and (or @todo (pos? @out))
                                        (commute out dec)))
                       (fnseq (. q (take)) consume)
                       (do
                         (. exec (shutdown))
                         (doseq x tasks)
                         nil)))]
     (consume)))
  ([f coll & colls]
   (pmap (fn [items] (apply f items))
         (let [encl-fn (fn [collseq]
                           (when (every? seq collseq)
                             (lazy-cons (map first collseq)
                                        (encl-fn (map rest collseq)))))]
           (encl-fn (cons coll colls))))))

(defn macroexpand-1 [form]
  (let [v (. clojure.lang.Compiler (isMacro (first form)))]
    (if v
      (apply @v (rest form))
      form)))

(defn macroexpand [form]
   (let [ex (macroexpand-1 form)
	 v  (. clojure.lang.Compiler (isMacro (first ex)))]
     (if v
       (macroexpand ex)
       ex)))

(defn create-struct [& keys]
   (. clojure.lang.PersistentStructMap (createSlotMap keys)))

(defmacro defstruct [name & keys]
  `(def ~name (create-struct ~@keys)))
  
(defn struct-map [s & inits]
  (. clojure.lang.PersistentStructMap (create s inits)))

(defn struct [s & vals]
  (. clojure.lang.PersistentStructMap (construct s vals)))

(defn accessor [s key]
   (. clojure.lang.PersistentStructMap (getAccessor s key)))

(defn subvec
  ([v start]
    (subvec v start (count v)))
  ([v start end]
    (. clojure.lang.RT (subvec v start end))))

(defn load [rdr]
  (. clojure.lang.Compiler (load rdr)))

(defn resultset-seq [#^java.sql.ResultSet rs]
  (let [rsmeta (. rs (getMetaData))
	idxs (range 1 (inc (. rsmeta (getColumnCount))))
	keys (map (comp keyword (memfn toLowerCase))
	       (map (fn [i] (. rsmeta (getColumnName i))) idxs))
	row-struct (apply create-struct keys)
	row-values (fn [] (map (fn [#^Integer i] (. rs (getObject i))) idxs))
	rows (fn []
	       (when (. rs (next))
		     (fnseq (apply struct row-struct (row-values)) rows)))]
    (rows)))

(defn to-set [coll]
  (loop [ret {} keys (seq coll)]
    (if keys
      (recur (if (contains? ret (first keys))
	       ret
	       (assoc ret (first keys) true))
	     (rest keys))
      ret)))

(defn distinct [coll]
  (keys (to-set coll)))

(defn filter-key [keyfn pred amap]
  (loop [ret {} es (seq amap)]
    (if es
      (if (pred (keyfn (first es)))
	(recur (assoc ret (key (first es)) (val (first es))) (rest es))
	(recur ret (rest es)))
      ret)))

(defn find-ns [sym]
  (. clojure.lang.Namespace (find sym)))

(defn create-ns [sym]
  (. clojure.lang.Namespace (findOrCreate sym)))

(defn remove-ns [sym]
  (. clojure.lang.Namespace (remove sym)))

(defn all-ns []
  (. clojure.lang.Namespace (all)))

(defn ns-name [#^clojure.lang.Namespace ns]
  (. ns (getName)))

(defn ns-map [#^clojure.lang.Namespace ns]
  (. ns (getMappings)))

(defn ns-unmap [#^clojure.lang.Namespace ns sym]
  (. ns (unmap sym)))

(defn export [syms]
  (doseq sym syms
   (.. *ns* (intern sym) (setExported true))))

(defn ns-exports [#^clojure.lang.Namespace ns]
  (filter-key val (fn [v] (and (instance? clojure.lang.Var v)
                       (. v (isExported))))
          (ns-map ns)))

(defn ns-imports [#^clojure.lang.Namespace ns]
  (filter-key val (partial instance? Class) (ns-map ns)))

(defn refer [ns-sym & filters]
  (let [ns (find-ns ns-sym)
	    fs (apply hash-map filters)
	    nsexports (ns-exports ns)
	    rename (or (:rename fs) {})
	    exclude (to-set (:exclude fs))
	    to-do (or (:only fs) (keys nsexports))]
    (doseq sym to-do
      (when-not (exclude sym)
	    (let [var (nsexports sym)]
	      (when-not var
	        (throw (new java.lang.IllegalAccessError (strcat sym " is not exported"))))
	      (. *ns* (refer (or (rename sym) sym) var)))))))

(defn ns-refers [#^clojure.lang.Namespace ns]
  (filter-key val (fn [v] (and (instance? clojure.lang.Var v)
			                (not= ns (. v ns))))
          (ns-map ns)))

(defn ns-interns [#^clojure.lang.Namespace ns]
  (filter-key val (fn [v] (and (instance? clojure.lang.Var v)
			                (= ns (. v ns))))
          (ns-map ns)))

(defn take-nth [n coll]
  (when (seq coll)
    (lazy-cons (first coll) (take-nth n (drop n coll)))))

(defn interleave [& colls]
  (apply concat (apply map list colls)))

(defn var-get [#^clojure.lang.Var x]
  (. x (get)))

(defn var-set [#^clojure.lang.Var x val]
  (. x (set val)))

(defmacro with-local-vars [name-vals-vec & body]
  `(let [~@(interleave (take-nth 2 name-vals-vec)
                       (repeat '(. clojure.lang.Var (create))))]
     (try
      (. clojure.lang.Var (pushThreadBindings (hash-map ~@name-vals-vec)))
      ~@body
      (finally (. clojure.lang.Var (popThreadBindings))))))

(defn ns-resolve [ns sym]
  (. clojure.lang.Compiler (resolveIn ns sym)))

(defn resolve [sym]
  (ns-resolve *ns* sym))

(defn array-map
	([] (. clojure.lang.PersistentArrayMap EMPTY))
	([& args] (new clojure.lang.PersistentArrayMap (to-array args))))

(export
	'(  load-file load
		list cons conj defn
		vector hash-map sorted-map sorted-map-by
		meta with-meta defmacro when when-not
		nil? not first rest second
		ffirst frest rfirst rrest
		= not= str strcat gensym cond
		apply list* delay lazy-cons fnseq concat
		and or + * / - == < <= > >=
		inc dec pos? neg? zero? quot rem
		complement constantly identity seq count
		peek pop nth contains? get
		assoc dissoc find keys vals merge merge-with
		scan touch
		key val
		line-seq sort sort-by comparator
		rseq symbol keyword name namespace locking .. ->
		defmulti defmethod remove-method
                binding find-var
		ref deref commute alter set ensure sync !
		agent agent-of agent-errors clear-agent-errors
		await await-for
		reduce reverse comp partial
		every? not-every? some not-any?
		map pmap mapcat filter take take-while drop drop-while
		zipmap
		cycle split-at split-with repeat replicate iterate range
		doseq  dotimes into
		eval import
		ns-unmap
		refer ns-refers ns-interns 
		in-ns
		;unintern
		into-array array
		make-proxy implement
		pr prn print println newline *out* *ns*  *print-meta* *print-readably*
		doto  memfn
        read *in* with-open
		time
		int long float double short byte boolean char
		aget aset aset-boolean aset-int aset-long aset-float aset-double aset-short aset-byte aset-char
		make-array alength to-array to-array-2d
		macroexpand-1 macroexpand
		max min
		bit-shift-left bit-shift-right
		bit-and bit-or bit-xor bit-not
		defstruct struct accessor create-struct struct-map
		subvec
		false? true?
		*warn-on-reflection*
		resultset-seq
		to-set distinct
		export ns-exports ns-imports ns-map
		identical?  instance?
		load-file in-ns find-ns
		filter-key find-ns create-ns remove-ns
		take-nth interleave var-get var-set with-local-vars
		ns-resolve resolve
		all-ns ns-name
		array-map
	))

