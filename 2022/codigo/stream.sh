#!/bin/sh

if [ "$1" = "bstrapper" ]; then
	if [ "$2" = "server" ]; then
    		java -cp Streaming/out overlay.NodeManager config overlay.xml server;
	else
    		java -cp Streaming/out overlay.NodeManager config overlay.xml;
	fi
fi
if [ "$1" = "node" ]; then
	if [ "$2" = "server" ]; then
    		java -cp Streaming/out overlay.NodeManager "$3" server;
	else
    		java -cp Streaming/out overlay.NodeManager "$2";
	fi
fi
if [ "$1" = "stream" ]; then
	java -cp Streaming/out streaming.OTTStreaming "$2";
fi
