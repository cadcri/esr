# Variables
JFLAGS = -d ./output/
JC = javac
JVM= java 
MAIN = main

# Default rule executed
all: 
	clean compile run

# Cleaning up any existing .class file
clean:
	rm -rf ./output/*

# Compiling the code
compile:
	mkdir -p output
	$(JC) $(JFLAGS) *.java Nodes/*.java Structs/*.java TCP/*.java UDP/*.java

# Running the program
run:
	$(JVM) -cp ./output/ $(MAIN)