# knx-utils

A set of utilities for working with the KNX bindings/integrations in OpenHAB and Home Assistant.
This project was created to easy the process of creating a configuration files for the OpenHAB KNX binding.
It also allows to create configuration files for Home Assistant.

## History / Project State

Note, the project is not complete. 
While working on it I lost interest in OpenHAB and switched to Home Assistant.
I created the configuration files once based on the analyzer output and never really had a need to complete the original intended functionality.


[![Branch & PR Validation](https://github.com/guw/knx-utils/actions/workflows/build.yml/badge.svg)](https://github.com/guw/knx-utils/actions/workflows/build.yml)


## How to Test and Contribute

0. Install (if not already presen) **Java 11**, **Maven 3.6** (or greater) and **Git**

1. Clone and build

```
git clone git@github.com:guw/knx-utils.git
cd knx-utils
mvn clean verify
```

2. Open in your favorite IDE (eg., in Eclipse via *File > Import... > Existing Maven Projects*, point *Root Directory* to `knx-utils`)

3. Run `knx-utils/cli/src/main/java/io/guw/knxutils/cli/KnxConvertCommand.java` with its `main` method

```
java -jar cli/target/knx-utils-cli.jar
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
cd <.../>knx-utils/semantic-analyzer
mvn -Pdownload-german-data generate-sources
```

## Analysis of Group Addresses

During analysis phase all group addresses will be analyzed.
The analysis will identify lights, switches, dimmers, rollershutters, thermostats, etc.
There is a heuristic that tries to identify all of this based on GA names, descriptions, connections, location and GA parent groups.
However, this is not always sufficient. 
If you encounter deficiencies please contribute solutions!

### Languages

The tool is capable of supporting multiple languages.
At this time only the German language support is implemented.
Please contribute support for your language!

### Patterns

The analysis is mostly based on best practices as defined by KNX.org (eg., *"[KNX Projektrichtlinien (KNX-Project-Design-Guidelines_de.pdf)](https://www.knx.org/wGlobal/wGlobal/scripts/accessDocument.php?forceDownload=0&document=%2FwAssets%2Fdocs%2Fdownloads%2FMarketing%2FFlyers%2FKNX-Project-Design-Guidelines%2FKNX-Project-Design-Guidelines_de.pdf)"*).
These best practices define patterns (*"charachteristics"*) of a KNX project.
If you would like to see additional characteristics being supported, please contribute an implementation of `KnxProjectCharacteristics`.

### Devices Categories

As part of the analysis, a GA will be annotated with a device category.
In order to detect a category, GA names and description will be considered.
The following table gives an introduction what is supported.

The logic is: GA name contains `<term>` *or* GA name starts with `<prefix>` *or* GA descriptions contains `[tag]`

| Category | Terms (DE, case-insensitive)                      | Prefix (DE, exact case) | Tags (DE, exact case) |
|----------|---------------------------------------------------|-------------------------|-----------------------|
| Light    | Lampe, Leuchte, Licht, Beleuchtung, Spots, Strahler (und zusammengesetzte Varianten)  | `L_`, `LD_`, `LDA_` | `[Licht]`  |
| Shutter  | Rollo, Rollladen (und weitere Varianten)          | `R_`                    | `[Rollo]`              |
| Heating  | Heizung (und  Varianten)                          | `H_`                    | `[Heizung]`            |

*Note: detection of additional functionality (eg., such as "a dimmable light") will be based on the availability of related GAs.*
If you have any suggestions for improvement, please don't hesitate and contribute.

#### Block of GAs

Once a device category is known, its primary GA and related GAs will be detected.
This detection happens based on GA DPT and name analysis.
As per recommendation of KNX.org, the following blocks will be searched once a primary GA is detected.

**Lights**

| GA      | Function                      | DPT   | Detected Suffixes (DE)      |
|---------|-------------------------------|-------|-----------------------------|
| x/y/z   | Switch (on/off)               | 1.001 | E/A, Ein/Aus                |
| x/y/z+1 | Dimming (brighter/darker)     | 3.007 | DIM, Dimmen, Heller/Dunkler |
| x/y/z+2 | Brightness (0..100%)          | 5.001 | WERT, Helligkeitswert       |
| x/y/z+3 | State of switch (on/off)      | 1.011 | RM, Status                  |
| x/y/z+4 | State if brightness (0..100%) | 5.001 | RM WERT, Status Wert        |

**Shutter**

| GA      | Function                   | DPT   | Detected Suffixes (DE) |
|---------|----------------------------|-------|------------------------|
| x/y/z   | Up/down                    | 1.001 |                   |
| x/y/z+1 | Stop                       |       |                   |
| x/y/z+2 | Position shutter (0..100%) |       |                   |
| x/y/z+3 | Position blade (0..100%)   |       |                   |
| x/y/z+4 | Shadow                     |       |                   |
| x/y/z+5 | Disable                    |       |                   |
| x/y/z+6 | State position shutter     |       |                   |
| x/y/z+7 | State position blade       |       |                   |

**Heating**

| GA      | Function                 | DPT | Detected Suffixes (DE) |
|---------|--------------------------|-----|------------------------|
| x/y/z   | Value                    |     |                   |
| x/y/z+1 | Actual temperature       |     |                   |
| x/y/z+2 | Base wanted temperature  |     |                   |
| x/y/z+3 | State                    |     |                   |
| x/y/z+4 | State wanted temperature |     |                   |
| x/y/z+5 | ...                      |     |                   |
| x/y/z+6 | ...                      |     |                   |
| x/y/z+7 | ...                      |     |                   |
| x/y/z+8 | ...                      |     |                   |
| x/y/z+9 | State mode               |     |                   |

