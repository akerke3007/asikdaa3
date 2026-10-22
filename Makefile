JAVAC ?= javac
JAVA ?= java
PYTHON ?= python3

.PHONY: compile demo test benchmark plots reproduce clean

compile:
	$(JAVAC) --release 17 -Xlint:all -Werror -d out @sources.txt

demo: compile
	$(JAVA) -cp out Main demo

test: compile
	$(JAVA) -cp out Main test

benchmark: compile
	$(JAVA) -Xms256m -Xmx256m -cp out Main benchmark

plots:
	$(PYTHON) scripts/plot_results.py

reproduce: test benchmark plots

clean:
	rm -rf out
