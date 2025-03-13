#!/bin/bash

echo "Compiling Java files..."
javac *.java

echo "Creating JAR manifest..."
echo "Main-Class: Alignment" > manifest.txt

echo "Creating JAR file..."
jar cvfm alignment.jar manifest.txt *.class *.java

echo "Cleaning up..."
rm manifest.txt *.class

echo "Done! You can now run the program using:"
echo "java -jar alignment.jar [options]" 