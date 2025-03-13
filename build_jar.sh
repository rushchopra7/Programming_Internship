#!/bin/bash

echo "Compiling Java files..."
javac -d out *.java

echo "Creating JAR manifest..."
echo "Main-Class: Alignment1.java" > manifest.txt

echo "Creating JAR file..."
jar cvfm align.jar manifest.txt -C out .

echo "Cleaning up..."
rm manifest.txt

echo "Done! You can now run the program using:"
echo "java -jar align.jar"
