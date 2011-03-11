(ns org.timmc.feedback
  "Behavioral simulator utility for sequential logic circuits.
   
   Specify a collection of logic blocks and registers, then advance the
   state of the world one clock cycle at a time.
   
   Each logic block is declared to take inputs from named wires and produce
   outputs on other named wires. (Values are not restricted to booleans or
   numbers; they are arbitrary.) The block is specified with a function that
   takes the values on the input wires in their declared order and produces
   a single output. From there, the unary output functions turn that value
   into the values that will be placed on their named outputs. There is a
   convenience syntax for having a single passthrough output function. The block
   name is not used for anything other than logging and debugging.
   
   Registers are named after wires they take their values from, and only
   advance on each clock cycle. To initialize the pipeline, all registers must
   be given initial values. After that, the step function may be used.
   
   Each clock cycle is atomic. The Pipeline object returned from create or step
   will have values on output wires that are consistent with the input to their
   logic blocks, with register values being used in place of wire values for
   inputs that name them. Note that a wire and a register with the same name
   will most likely not have the same value. However, the register's next value
   will be the wire's current value.
   
   There are currently no checks for the sanity of the pipeline you construct,
   besides ensuring that there are no loops of logic blocks that are not
   broken up by registers. If you specify named wires or registers that do not
   match inputs or outputs, you may receive unexpected nil values or even
   exceptions. (Future versions of this utility may check for this condition.)
   If you have an inconsistent number of registers along any two datapaths,
   you will not be warned. (This is sometimes an intentional design decision.)
   
   The API consists of create, add, init, step, read-register, and read-wire."
  (:require [clojure.set :as set])
  (:use [loom.graph :only (digraph)]
        [loom.alg :only (topsort)]))

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
   ;; computed values
   ^{:doc "Ordered collection of Blocks in the order they should be run."}
   update-order
   ])

;;;; API accessors

(defn- require-init
  "Throw error if not initialized, running the thunk to get an action name
   for the message."
  [^Pipeline p, action-name-thunk]
  (when-not (.initialized? p)
    (throw (IllegalStateException.
            (str "Cannot " (action-name-thunk) " before initialization.")))))

(defn read-register
  "Return value in register, if initialized."
  [^Pipeline p, reg-kw]
  (require-init p #(str "read register " (name reg-kw)))
  (-> p (.registers) reg-kw))

(defn read-wire
  "Return value on wire, if initialized."
  [^Pipeline p, wire-kw]
  (require-init p #(str "read wire " (name wire-kw)))
  (-> p (.wires) wire-kw))

;;;; Topology calculations

(defn- find-input-block-name
  "Find the name of the source logic block for the named wire.
   If there is a register on this wire, return nil."
  [^Pipeline p, wire-kw]
  (if (contains? (.registers p) wire-kw)
    nil
    (key (first (filter #(contains? (.outputs ^Block (val %)) wire-kw)
                        (.blocks p))))))

(defn- block-depends
  "Find all block-block dependencies for the given block name.
   Result is a collection of vectors [b some-block].
   
   Independent blocks that only read from registers and are never read from
   by other blocks will return an empty collection and must be handled
   carefully if they are to be placed in the graph."
  [^Pipeline p, name]
  (->> (.inputs ^Block ((.blocks p) name))
       (map (partial find-input-block-name p) ,,,)
       (filter (complement nil?) ,,,)
       (map (partial vector name) ,,,)))

(defn- block-graph
  "Return the graph of Block records."
  [^Pipeline p]
  (let [parts (for [bk (keys (.blocks p))]
                (let [deps (block-depends p bk)]
                  ;; non-dependent nodes handled specially for addition to graph
                  (if (seq deps)
                    deps
                    [bk])))]
    (apply digraph (apply concat parts))))

(defn- sorted-block-names
  "Return the names of blocks in an order appropriate for updating.
   Throw error if cycles detected."
  [^Pipeline p]
  (let [g (block-graph p)
        sorted (reverse (topsort g))]
    (when (nil? sorted)
      (throw (Exception. "Cycle detected in logic blocks. Add registers.")))
    sorted))

(defn- ^Pipeline sort-blocks
  "Fill in the .update-order field on a Pipeline with registers."
  [^Pipeline p]
  (assoc-in p [:update-order] (replace (.blocks p) (sorted-block-names p))))

;;;; Unchecked modifiers

(defn- ^Pipeline merge-registers
  "Add registers and values (as map) without recomputing."
  [^Pipeline p, regset]
  (update-in p [:registers] merge regset))

(defn- ^Pipeline compute-1
  "Compute the new outputs of one block. Assumes dependencies are clean."
  [^Pipeline p, ^Block b]
  (let [main-val (apply (.process b) (replace (merge (.wires p) (.registers p))
                                              (.inputs b)))
        out-vals ((apply juxt (vals (.outputs b))) main-val)
        out-pairs (map vector (keys (.outputs b)) out-vals)]
    (update-in p [:wires] into out-pairs)))

(defn- ^Pipeline compute-wires
  "Recompute the wire values on a dirty pipeline."
  [^Pipeline p]
  (reduce compute-1 p (.update-order p)))

;;;; Running

(defn ^Pipeline reset
  "Set new values for 0 or more registers (as a map of name keys to values)
   and compute the new wire values."
  [^Pipeline p, regsets]
  (require-init p #(str "set registers"))
  (compute-wires (merge-registers p regsets)))

(defn ^Pipeline step
  "Step the simulation forward by one cycle."
  [^Pipeline p]
  (require-init p #(str "step simulation"))
  (reset p (select-keys (.wires p) (keys (.registers p)))))

;;;; Construction

(defn- ^Pipeline add-unchecked
  "Same as add, but doesn't read or write the .initialized? field."
  [^Pipeline p, name, f, inputs, outputs]
  (let [outputs (if (map? outputs) outputs {outputs identity})
        more-wires (map #(vector % nil) (concat inputs (keys outputs)))
        with-wires (update-in p [:wires] merge (into {} more-wires))]
    (assoc-in with-wires [:blocks name] (Block. inputs f outputs))))

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
  (assoc-in (add-unchecked p name f inputs outputs) [:initialized?] false))

(defn ^Pipeline init
  "Initialize a pipeline by specifying all registers (using a map of
   register name keys to initial values) and updating the wire values.
   The resulting pipeline is ready to be used."
  [^Pipeline uninit, init-pairs]
  (let [with-reg (merge-registers uninit init-pairs)
        with-updaters (sort-blocks with-reg)
        consistent (compute-wires with-updaters)]
    (assoc-in consistent [:initialized?] true)))

(defn ^Pipeline create
  "Create a Pipeline, optionally with the specified logic blocks, each being
   the same collection that `add` accepts as arguments."
  [& blocks]
  (reduce #(apply add-unchecked %1 %2)
          (Pipeline. {} {} {} false nil)
          blocks))

;;;; Useful addons

(defmethod print-method Block
  [^Block bl, writer]
  (print-method {:in (.inputs bl) :out (keys (.outputs bl))} writer))

(defmethod print-method Pipeline
  [^Pipeline pl, writer]
  (print-method (select-keys pl [:wires :registers :initialized? :update-order])
                writer))
