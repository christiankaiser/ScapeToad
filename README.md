# Welcome to ScapeToad


ScapeToad is an easy to use Java application for cartogram creation. A cartogram is a transformed map where the polygons are proportional to the value of a given statistical variable, e.g. the population. More information is available on the project web site at [chorogram.choros.ch/scapetoad](http://chorogram.choros.ch/scapetoad/) or on the Github project site.


## Requirements

ScapeToad is an application which requires quite big computation resources. We have made quite an effort to make the cartogram making accessible. However, ScapeToad needs at least 512 MB of memory. If you want to produce high quality cartograms, it is preferable to have a fast processor. ScapeToad needs at least Java 1.4.


## License

ScapeToad is an open-source project released under the GNU Public License. You can find the complete license in the separate LICENSE.txt file.


## Contents of this package

ScapeToad is a Java application and can be used on any computer with an installed Java Virtual Machine.

Additionnaly, we also have released a separate binary for Windows packaged into an EXE file.

For instructions how to install and run the programm, see below.


## Compilation

Unless you want to modify ScapeToad, there is no need for compilation. However, if you want to make changes, the compile process should be straightforward by using Ant. With Ant installed, simply cd to the src directory and run ant. Resulting JAR file will be in dist directory.

There is also a jsmooth project file inside win directory for creating a Windows 
executable using JSmooth (http://jsmooth.sourceforge.net/).


## Install and run ScapeToad

__Windows__: Just drag the ScapeToad.exe file inside the Windows folder on your hard drive. Normally, programs should be installed into the Program files folder on the C: drive, but this is not a pre-requisite for running ScapeToad.

__Other platforms, including macOS and Linux__: Use the multi-platform JAR file in the Unix folder. Just copy the file ScapeToad-vXX.tgz located in binaries/jvm to your drive, and decompress the file. After, open your Terminal, and launch the JAR file like this:

	java -Xmx512m -jar /your/install/folder/ScapeToad.jar

Usually, you don't need to type the whole path to your ScapeToad.jar file. Instead, simply drag the ScapeToad.jar file onto the Terminal window.

There is also a launch script (ScapeToad.sh) if you prefer that one.


## Contribute

You are welcome to make contributions to ScapeToad. Simply fork the repo and send your pull request. 
 
 