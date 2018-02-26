javac -cp gson-2.2.4.jar DataModel/*java
jar xf gson-2.2.4.jar
jar cmf MANIFEST.MF iReveal.jar com/* DataModel/*.class