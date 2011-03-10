
SHELL:=/bin/bash

build:
	lein compile | grep -v 'at clojure.'

test:
	lein test | grep -v 'at clojure.' | grep -v '^    clojure.'

run:
	lein run

reflections:
	lein clean
	lein compile | sed 's|Reflection warning, ||' | sed "s| can't be resolved\\.||"

todo:
	grep 'TODO' -nR */ || true
	grep 'FIXME' -nR */ || true
	grep 'XXX' -nR */ || true

clean: pkg-clean

PROJNAME := pipeline
PACKAGE_FILE := $(PROJNAME).tar.gz
PKGDIR := pkg/$(PROJNAME)

pkg: pkg-clean
	mkdir -p $(PKGDIR)/
	cp README $(PKGDIR)/
	cp -r src/ $(PKGDIR)/src
	cp -r test/ $(PKGDIR)/test
	find ./pkg -name '*~' -delete
	tar -czf $(PACKAGE_FILE) --directory pkg/ $(PROJNAME)/

pkg-clean:
	rm -f $(PROJNAME).tar.gz
	rm -rf pkg
	lein clean

.PHONY: pkg build test run todo doc

.SILENT:

