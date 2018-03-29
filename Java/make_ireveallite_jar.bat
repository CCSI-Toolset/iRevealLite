javac -cp gson-2.8.0.jar DataModel/*java
jar xf gson-2.8.0.jar
jar cmf MANIFEST.MF iRevealLite.jar com/* DataModel/*.class