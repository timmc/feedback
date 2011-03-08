(ns org.timmc.pipeline
  "Logic pipeline manager.

   Specify a collection of control blocks and registers, then advance the
   state of the world one clock cycle at a time. Query the halt? function
   to determine when the computation is finished.")

;;;; Implementation

(defrecord ^{:doc "A single logic block."}
    Block
  [
   ^{:doc "Core processing function"}
   process
   ^{:doc "Map of registers to output functions"}
   outputs
   ^{:doc "Whether this is a :halt block."}
   halt?
   ])

(defrecord ^{:doc "Implementation for pipeline methods."}
    Pipeline
  [
   ^{:doc "Map of registers to their values."}
   registers
   ^{:doc "Map of keys of init registers to their initial values."}
   initials
   ^{:doc "Map of name keys to Block records."}
   blocks
   ^{:doc "Vector of keys of halt blocks."}
   halts
   ^{:doc "True if Pipeline has been initialized."}
   started?
   ])

;;;; Constants

(def ^{:doc "Option keyword indicating a halt block."}
  sig-halt :halt)

(def ^{:doc "Option keyword indicating an initialization block."}
  sig-init :init)

;;;; Construction

(defn validate
  "Throw an error if this pipeline is inconsistent."
  [^Pipeline p]
  ;; TODO consistency check for Pipeline
  )

(defn- add-unchecked
  "Unchecked addition of a logic block.")

(defn add
  "Define a logic block, its dependencies, and its exports.
   - name: a keyword naming this logic block uniquely
   - f: function that receives the dependency values as arguments in order
   - exports: a map of keywords (naming registers) to functions that will
     take the return value of f and produce values for those registers.
     Alternatively, a single bare keyword can be provided, and will be treated
     as {kw identity}
   - options: keywords that mark this logic block as having special properties.
     Legal values: :halt and :init (described in namespace docs.)"
  [^Pipeline p, name, f, depends, exports, & options]
  
  (Pipeline. (into (.registers p) )
             false))

(defn create
  "Create a Pipeline with the specified blocks. Each argument must be a vector
   containing the args one would pass to add."
  [& blocks]
  (reduce #(apply add %1 %2)
          (Pipeline. {} {} {} [] false)
          blocks))

(defn initialize
  "Load initialization registers and flush them through the logic pipeline.
   The resulting Pipeline is in a consistent state and ready to run."
  [& init-pairs]
  ;; TODO: determine necessary cycle count n
  ;; TODO: n times: step-unchecked once, reset init registers
  ;; TODO: mark initialized
  )

;;;; Running

(defn step
  "Step the simulation forward by one cycle."
  [^Pipeline p]
  (let [old (.registers p)]
    (.blocks p)))

;;; components:

;;; Need a way to hold :state for x clock cycles.
;;; If the dependencies are explicit, can compute x.

;;; Need to make explicit which components are state and halt.
