[![Build Status](https://travis-ci.org/jbloemendal/hst-scaffold.svg?branch=master)](https://travis-ci.org/jbloemendal/hst-scaffold)
![HST-Scaffold](https://raw.githubusercontent.com/jbloemendal/hst-scaffold/master/logo.png)
HST-Scaffold
============

Scaffold your projects Hippo Site Toolkit configuration from text file.

Build
```
mvn clean verify
mv hst-scaffold/target/hsd-jar-with-dependencies.jar hsd.jar
```

Usage:
```
java -jar hsd.jar [options] [args]

Options
-h     --help               Show help
-b     --build              Build configuration from scaffold.
-c     --configuration      Custom configuration file.
-u     --update             Update configuration from scaffold
-s     --scafold            Build scaffold from existing project configuration (reverse).
-r     --rollback           Rollback configuration changes.

```