#!/bin/sh

rm -rf out/*
javac -d Streaming/out Streaming/src/overlay/*.java Streaming/src/overlay/TCP/*.java Streaming/src/overlay/state/*.java Streaming/src/overlay/bootstrapper/*.java Streaming/src/streaming/*.java Streaming/src/streaming/UDP/*.java
javac -d CDN/out CDN/src/overlay/*.java CDN/src/overlay/TCP/*.java CDN/src/overlay/state/*.java CDN/src/overlay/bootstrapper/*.java CDN/src/streaming/*.java CDN/src/streaming/UDP/*.java
