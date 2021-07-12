# wmf2svg - WMF to SVG Converting Tool & Library for Java

This project's goal is to make tool & library for converting wmf to svg.

## Example

```
java -jar wmf2svg-[version].jar [options...] [wmf filename] [svg filename]
```

## Options

- -compatible: output IE9 compatible style. but it's dirty and approximative.
- -replace-symbol-font: replace SYMBOL Font to serif or sans-serif Unicode SYMBOL.

If you need to compress by gzip, you should use .svgz suffix as svg filename.

It now requires Java 6.0 or later.

## Maven repository

```xml
<groupId>net.arnx</groupId>
<artifactId>wmf2svg</artifactId>
```

## Build

This project uses Gradle to build a wmf2svg jar file.

```
./gradlew jar
```

## History

- 2021-07-12: Fix a bug about images with palette.
- 2021-06-30: Fix a bug that occured when using Symbol type fonts.
- 2018-11-24: Changed to gradle build system.
- 2015-07-04: wmf2svg now requires Java 6.0 or later.
- 2014-07-19: Fixed pie method bug.
- 2014-02-23: Added -replace-symbol-font option.
- 2013-09-29: Fixed a invalid css property.
- 2012-08-28: Improved arc radious and Fixed negative size image bug.
- 2012-07-05: Improved arc problem, and fixed TextOut? background position bug.
- 2012-02-11: Improved arc problem, and fixed font-size bug.
- 2011-09-28: Fixed restoreDC and cyrillic bug. And add wmf writer function.
- 2011-08-20: Some bugs fixed.
- 2011-03-10: Arc bug fixed and extTextOut is enhanced.
- 2011-01-08: Arc bug fixed and add fillRgn/paintRgn support.
- 2010-12-01: Some bugs fixed.
- 2010-11-20: Text position bug fixed.
- 2010-09-11: Some bugs fixed.
- 2009-05-24: wmf2svg version 0.8.3 is supported on Google App Engine/Java.

Copyright (c) 2007-2021 Hidekatsu Izuno, Shunsuke Mori All right reserved.
