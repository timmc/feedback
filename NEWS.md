# Changelog

## v0.4.0 (???)

Further debugging-support enhancement.

### Features

* Add API methods all-registers and all-wires.

## v0.3.0 (2011-04-02)

Improve debugging output.

### Enhancements

* If a processor function (either main or auxiliary output) throws an exception,
  the exception will be wrapped with a message that indicates what block and
  output were at fault.

## v0.2.0 (2011-03-13)

First release. Fully working logic pipeline, exporting:
* create, add, init
* reset, step
* read-register, read-wire
* print methods
