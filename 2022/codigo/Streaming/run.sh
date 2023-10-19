#!/bin/sh

rm -rf out/*
javac -d out src/overlay/*.java src/overlay/TCP/*.java src/overlay/state/*.java src/overlay/bootstrapper/*.java src/streaming/*.java src/streaming/UDP/*.java

if [ "$1" = "bstrapper" ]; then
	if [ "$2" = "server" ]; then
    		java -cp out overlay.NodeManager config overlay.xml server;
	else
    		java -cp out overlay.NodeManager config overlay.xml;
	fi
fi
if [ "$1" = "node" ]; then
	if [ "$2" = "server" ]; then
    		java -cp out overlay.NodeManager "$3" server;
	else
    		java -cp out overlay.NodeManager "$2";
	fi
fi
if [ "$1" = "stream" ]; then
	java -cp out streaming.OTTStreaming "$2";
fi
if [ "$1" = "tmp" ]; then
	java -cp out streaming.Tmp "$2";
fi
