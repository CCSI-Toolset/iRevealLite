mkdir bin

javac -d bin -cp ".;lib/gson-2.2.4.jar" ../DataModelProject/DataModel/*.java
javac -d bin -cp ".;bin/" src/gov/pnnl/reveal/lite/util/*.java
javac -d bin -cp ".;bin/" src/gov/pnnl/reveal/lite/model/barracuda/*.java

cd bin
jar xf ../lib/*.jar 
jar cmf ../MANIFEST.MF ../iREVEAL.jar DataModel/* gov/* com/*

cd ..
