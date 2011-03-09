(ns org.timmc.pipeline.core
  "Logic pipeline manager.

   Specify a collection of logic blocks and registers, then advance the
   state of the world one clock cycle at a time.

   Each logic block is declared to take inputs from named wires and produce
   outputs on other named wires. (Values are not restricted to booleans or
   numbers; they are arbitrary.) The block is specified with a function that
   takes the values on the input wires in their declared order and produces
   a single output. From there, the unary output functions turn that value
   into the values that will be placed on their named outputs. There is a
   convenience syntax for having a single passthrough output function.
   
   Registers are named after wires they take their values from, and only
   advance on each clock cycle. To initialize the pipeline, all registers must
   be given initial values. After that, the step function may be used.

   Each clock cycle is atomic. The Pipeline object returned from create or step
   will have values on output wires that are consistent with the input to their
   logic blocks, with register values being used in place of wire values for
   inputs that name them.

   There are currently no checks for the sanity of the pipeline you construct.
   A) If you specify named wires or registers that do not match inputs or
      outputs, you may receive unexpected nil values or even exceptions.
   B) If you have an inconsistent number of registers along any two datapaths,
      you will not be warned.
   C) If you manage to construct a cycle of blocks that is not broken up by
      registers, you will almost certainly encounter an infinite loop when you
      run the pipeline.
   Future versions of this utility may check for cases A and C."
  (require [clojure.set :as set]))

;;;; Implementation

(defrecord ^{:doc "A single logic block."}
    Block
  [
   ^{:doc "Names of inputs in order, as a vector."}
   inputs
   ^{:doc "Core processing function, which takes the inputs in order."}
   process
   ^{:doc "Map of output names to unary functions, each of which takes the
           output of .process as input."}
   outputs
   ])

(defrecord ^{:doc "Implementation for pipeline methods."}
    Pipeline
  [
   ^{:doc "Map of wire names to values."}
   wires
   ^{:doc "Map of register names to values."}
   registers
   ^{:doc "Map of block names to Block records."}
   blocks
   ^{:doc "True if Pipeline has been initialized (flushed with good data.)"}
   initialized?
   ])

;;;; Accessors

(defn- require-init
  "Throw error if not initialized, running the thunk to get an action name
   for the message."
  [^Pipeline p, action-name-thunk]
  (when-not (.initialized? p)
    (throw (IllegalStateException.
            (str "Cannot " (action-name-thunk) " before initialization.")))))

(defn peek-register
  "Return value in register, if initialized."
  [^Pipeline p, reg-kw]
  (require-init #(str "peek register " (name reg-kw)))
  (-> p (.registers) reg-kw))

(defn peek-wire
  "Return value on wire, if initialized."
  [^Pipeline p, wire-kw]
  (require-init #(str "peek wire " (name wire-kw)))
  (-> p (.wires) wire-kw))

;;;; Running

(defn- ^Block find-input-block
  "Find the source logic block for the named wire.
   If there is a register on this wire, return nil."
  [^Pipeline p, wire-kw]
  (if (contains? (.registers p) wire-kw)
    nil
    (some #(contains? (.outputs ^Block %) wire-kw) (.blocks p))))

(defn- ^Pipeline compute-1
  "Compute the new outputs of one block. Assumes dependencies are clean."
  [^Pipeline p, ^Block b]
  (let [main-val (apply (.process b) (replace (.wires p) (.inputs b)))
        out-vals ((juxt (vals (.outputs b))) main-val)]
    (update-in p [:wires] (partial map assoc) (keys (.outputs b)) out-vals)))

(defn- ^Pipeline compute-wires-worklist
  "Use worklist to perform actual work for compute-wires.
   `remaining` is the set of wire names left to compute."
  [^Pipeline p, remaining]
  (if-let [wire (first (seq remaining))]
    (if-let [block (find-input-block p wire)]
      (let [inputs (.inputs block)]
        (if (empty? (set/intersection (set (keys inputs)) remaining))
          (recur (compute-1 p block) (disj remaining wire))
          ;; TODO case where dependencies not met
          )))
    p))

(defn- ^Pipeline compute-wires
  "Recompute the wire values on a dirty pipeline."
  [^Pipeline p]
  (compute-wires-worklist (set (keys (.wires p)))))

(defn ^Pipeline reset
  "Set new values for 0 or more registers (as a map of name keys to values)
   and compute the new wire values."
  [^Pipeline p, regsets]
  (compute-wires (update-in p [:registers] merge regsets)))

(defn ^Pipeline step
  "Step the simulation forward by one cycle."
  [^Pipeline p]
  (let [newregs (select-keys (.wires p) (keys (.registers p)))]
    (compute-wires (assoc-in p [:registers] newregs))))

;;;; Construction

(defn ^Pipeline add
  "Define a logic block, its inputs, functions, and outputs.
   - name: a keyword naming this logic block uniquely
   - f: function that receives the input values as arguments in order
   - inputs: a collection of keywords naming wires to be use as inputs to f
   - outputs: a map of keywords (naming own outputs) to functions that will
     take the return value of f and produce values for output.
     Alternatively, a single bare keyword can be provided, and will be treated
     as {kw identity}"
  [^Pipeline p, name, f, inputs, outputs]
  (let [outputs (if (seq? outputs) outputs {outputs identity})]
    ;; FIXME: should really add wire names in at this point
    (assoc-in p [:blocks name] (Block. inputs f outputs))))

(defn ^Pipeline initialize
  "Initialize a pipeline by specifying all registers (using a map of
   register name keys to initial values) and updating the wire values.
   The resulting pipeline is ready to be used."
  [^Pipeline p, init-pairs]
  (assoc-in (reset p init-pairs) [:initialized?] true))

(defn ^Pipeline create
  "Create a Pipeline, optionally with the specified logic blocks, each being
   the same collection that `add` accepts as arguments."
  [& blocks]
  (reduce #(apply add %1 %2)
          (Pipeline. {} {} {} false)
          blocks))
