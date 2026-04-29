# wmf2svg - WMF/EMF to SVG Converting Tool & Library for Java

This project's goal is to make tool & library for converting wmf/emf to svg.

## Example

```
java -jar wmf2svg-[version].jar [options...] [wmf/emf filename] [svg/svgz filename]
```

## Options

- -compatible: output IE9 compatible style. but it's dirty and approximative.
- -replace-symbol-font: replace SYMBOL/Wingdings[23]? Font to serif Unicode symbols.

If you need to compress by gzip, you should use .svgz suffix as svg filename.

It now requires Java 8.0 or later.

## Maven repository

```xml
<groupId>net.arnx</groupId>
<artifactId>wmf2svg</artifactId>
<version>0.10.2</version>
```

## Build

This project uses Maven to build a wmf2svg jar file.

```
mvn package
```

## History

- 2026-04-30: Improve EMF+ GetDC, raw/indexed/high-depth/compressed bitmap images, continued object records, curve, anti-alias mode, pixel offset/compositing quality/compositing mode, terminal-server graphics state/clip including compressed clip, container state/transform, text/layout clipping/text rendering hint/text contrast/hotkey prefix/right-to-left/tracking/font unit preservation, path fill mode, clipping/offset clip/xor/complement clip combine, region/empty/infinite/intersect/union/xor/exclude/complement region, world/page transform, rendering origin, interpolation/image rendering mode, stroke-fill path, pen unit/dash/dash cap/cap/join/miter/transform data, image source rectangle clipping, linear gradient/preset color/horizontal and vertical blend factor/gamma correction/transform/wrap mode brush, path gradient/preset color/blend factor/focus scale/gamma correction/elliptical bounds/wrap mode brush, hatch brush, and texture brush transform/gamma correction support.
- 2026-04-29: Fix some bugs
- 2026-04-28: Implements an EMF support (by OpenAI Codex and human instructions)
- 2026-04-26: Migrated to Maven build system. And some TODO implemented.
- 2021-07-12: Fix a bug about images with palette.
- 2021-06-30: Fix a bug that occurred when using Symbol type fonts.
- 2018-11-24: Changed to gradle build system.
- 2015-07-04: wmf2svg now requires Java 6.0 or later.
- 2014-07-19: Fixed pie method bug.
- 2014-02-23: Added -replace-symbol-font option.
- 2013-09-29: Fixed a invalid css property.
- 2012-08-28: Improved arc radius and Fixed negative size image bug.
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

Copyright (c) 2007-2026 Hidekatsu Izuno, Shunsuke Mori All right reserved.
