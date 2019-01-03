# knx-openhab-utils

A set of utilities for working with the KNX binding in OpenHAB.
This project was created to easy the process of creating a configuration files for the KNX binding.

[![Build Status](https://travis-ci.org/guw/knx-openhab-utils.svg?branch=master)](https://travis-ci.org/guw/knx-openhab-utils)


## How to Test and Contribute

0. Install (if not already presen) **Java 11**, **Maven 3.6** (or greater) and **Git**

1. Clone and build

```
git clone git@github.com:guw/knx-openhab-utils.git
cd knx-openhab-utils
mvn clean verify
```

2. Open in your favorite IDE (eg., in Eclipse via *File > Import... > Existing Maven Projects*, point *Root Directory* to `knx-openhab-utils`)

3. Run `knx-openhab-utils/cli/src/main/java/io/guw/knxopenhabutils/cli/KnxConvertCommand.java` with its `main` method

```
java -jar cli/target/knx-openhab-utils-cli.jar
```

## ETS Project Converter

The programming tool of choice for KNX is the ETS software.
It contains all essential data of a KNX installation.
The first available utility is a converter for creating OpenHAB configuration information from ETS data.

The process is easy:
1. Export a single project from ETS into a `.knxproj` file.
2. Run converter to create OpenHAB files.
3. Place generated files into OpenHAB configuration directory.

The converter applies semantic processing on the exported KNX project.
This semantic processing helps generating proper OpenHAB configuration files.
It understands keywords (tags) used in names and descriptions of group addresses and device communication objects as well as patterns found in group addresses.


### Status

* This is a proof of concept. Lots of missing features.
* Only German names/descriptions supported so far
* No OpenHAB files generated yet. Still working on analyzing a KNX project.

*Please help improve the semantic processing by submitting pull requests.*


### Setup German Decompounding

In order to use enhanced language features dictionaries need to be downloaded from [uschindler/german-decompounder](https://github.com/uschindler/german-decompounder).
They cannot be distributed due to licensing concerns.

```
cd <.../>knx-openhab-utils/semantic-analyzer
mvn -Pdownload-german-data generate-sources
```

