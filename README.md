# knx-openhab-utils

A set of utilities for working with the KNX binding in OpenHAB.
This project was created to easy the process of creating a configuration files for the KNX binding.

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

Please help improve the semantic processing by submitting pull requests.
