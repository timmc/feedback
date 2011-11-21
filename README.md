# org.timmc.feedback

Behavioral simulator utility for sequential logic circuits.

**Warning: This library needs to be rewritten. The current version's
wire-and-block model contains a misconception that will make certain circuits
impossible to code.** I've forgotten the precise details, but it has something
to do with the overly clever feature of registers taking their value from
wires of the same name.

Simulate a sequential circuit at a high level by defining combinational logic
blocks, their interconnections, and the placement of registers. After
initializing all the registers, step the simulation forward one logical clock
cycle at a time.

Each logic block can take multiple inputs and produce multiple outputs, with
support for shared computation among output functions. Each output is uniquely
named (usually with a :keyword); these names define the inputs available to
other logic blocks. (In the nomenclature of org.timmc.feedback, these are
called "wires".) Registers are specified at the initialization step. Any
register with the same name as a wire will intercept the value and accept it
on the next tick of the clock. Logic block inputs matching registers always
recieve the value of the register in the current cycle.

## Usage

Following is a contrived example using the hailstone sequence (see
[Collatz conjecture](https://secure.wikimedia.org/wikipedia/en/wiki/Collatz_conjecture)
for details) implemented as a logic circuit. The :n wire is the current sequence
value, and a register is placed on that wire with the starting value. Each logic
block in this example only produces one output. (See the documentation for `add`
for more options.) Note that the :done block provides a boolean output, but that
all other blocks are providing numerical output. org.timmc.feedback does not
constrain you to boolean logic—output may be of any type, including collections.

    (require '[org.timmc.feedback :as circuit])
    
    (def hailstone
      (circuit/create
       [:next #(if (zero? %1) %2 %3) [:parity :half :trinc] :n]
       [:done #(= 1 %) [:n] :halt]
       [:decoder #(mod % 2) [:n] :parity]
       [:down #(quot % 2) [:n] :half]
       [:up1 #(* 3 %) [:n] :tri]
       [:up2 #(inc %) [:tri] :trinc]))
    
    (def hailstones-27
      (iterate circuit/step (circuit/init hailstone {:n 27})))
    
    (circuit/read-register (nth hailstones-27 45) :n)
    ;; => 502

    (circuit/read-register (nth hailstones-27 111) :n)
    ;; => 1
    (circuit/read-wire (nth hailstones-27 111) :halt)
    ;; => true

Please feel free to contact me with any suggestions. See the TODO.md file for
ideas I'm already considering.

## License

Source copyright (c) 2011 Tim McCormack
Source licensed under GNU General Public License v3.0 and Eclipse Public License
v1.0.

