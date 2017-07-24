REM iREVEAL make.bat

REM

REM This file is a reproductions of the iREVEAL 
REM Jenkins build at LBNL. It has been tested on

REM    - 64 bit windows 7 installation (required)

REM

REM Pre-requisites:

REM    - Ant 1.9.6



if NOT EXIST iREVEAL\lib. (
  MKDIR iREVEAL\lib 
)

if NOT EXIST iREVEAL\lib\gson-2.2.4.jar. (
  curl http://central.maven.org/maven2/com/google/code/gson/gson/2.2.4/gson-2.2.4.jar -o iREVEAL\lib\gson-2.2.4.jar 
)

ant -f iREVEAL\build.xml && cd iREVEAL && 7z.exe a iREVEAL.zip iREVEAL.jar config ../docs/*iR*.pdf && cd ..