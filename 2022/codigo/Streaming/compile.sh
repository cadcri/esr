#!/bin/sh

rm -rf out/*
javac -d out src/overlay/*.java src/overlay/TCP/*.java src/overlay/state/*.java src/overlay/bootstrapper/*.java src/streaming/*.java src/streaming/UDP/*.java
