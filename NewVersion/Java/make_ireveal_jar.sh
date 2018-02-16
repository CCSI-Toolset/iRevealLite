#!/bin/sh
jar xf gson-2.2.4.jar
jar cmf MANIFEST.MF iReveal.jar com/* DataModel/*.class