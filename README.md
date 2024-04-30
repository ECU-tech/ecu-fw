<div align="center">

<b>GPL open-source ECU</b>

[![Release](https://img.shields.io/github/v/release/ECU-tech/ecu-fw?style=flat)](https://github.com/ECU-tech/ecu-fw/releases/latest) ![Last Commit](https://img.shields.io/github/last-commit/ECU-tech/ecu-fw?style=flat)
[![Unit Tests](https://img.shields.io/github/actions/workflow/status/ECU-tech/ecu-fw/build-unit-tests.yaml?label=Unit%20Tests&branch=master)](https://github.com/ECU-tech/ecu-fw/actions/workflows/build-unit-tests.yaml)
![GitHub commits since latest release (by date)](https://img.shields.io/github/commits-since/ECU-tech/ecu-fw/latest?color=blueviolet&label=Commits%20Since%20Release)

</div>

# ECU-tech: Free Open Source ECU

Welcome to ECU-tech, a project intended to provide OEM quality open source engine controls and focus on user-friendly design, stability and ease of use. 

At present ECU-tech shares a large amount of its core with the rusEFI project and most rusEFI boards are compatible with the ECU-tech software

# What Do We Have Here?
 * [Firmware](/firmware) - Source code for open source engine control unit for stm32 chips
 * [ECU-tech console](/java_console) - ECU debugging/development software
 * [Simulator](/simulator) - Windows or Linux version of firmware allows exploration without any hardware
 * [Unit Tests](/unit_tests) - Unit tests of firmware
 * [Misc tools](/java_tools) - Misc development utilities
 * [misc/Jenkins](/misc/jenkins) - Continuous integration scripts

# External Links

 * [Wiki](https://wiki./)
 * [Forum](https://www/forum)
 * [Facebook](https://www.facebook.com/)
<!--
 * [YouTube](https://www.youtube.com/)
-->

## Getting Started

Clone the repository:  
`git clone https://github.com/ECU-tech/ecu-fw.git`

Initialize the checkout:  
`git submodule update --init`

## Building

See [firmware/gcc_version_check.c](firmware/gcc_version_check.c) for the recommended version of GCC.

Check out https://rusefi.com/forum/viewtopic.php?f=5&t=9

# Release Notes

See [the changelog](firmware/CHANGELOG.md), or [by release](https://github.com/ECU-tech/ecu-fw/releases).
