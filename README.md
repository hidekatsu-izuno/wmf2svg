# wmf2svg - WMF/EMF Converting Tool & Library for Java

This project's goal is to provide a Java tool and library for reading WMF/EMF files and rendering or writing them through GDI-compatible backends.

## Command line

```
java -jar wmf2svg-[version].jar [options...] [wmf/emf filename] [svg/svgz/png/jpg/jpeg filename]
```

The output format is selected by the output filename suffix.

- `.svg`: write SVG.
- `.svgz`: write gzip-compressed SVG.
- `.png`: render to PNG using Java2D/ImageIO.
- `.jpg` or `.jpeg`: render to JPEG using Java2D/ImageIO.

## Options

- -compatible: output IE9 compatible style. but it's dirty and approximative.
- -replace-symbol-font: replace SYMBOL/Wingdings[23]? Font to serif Unicode symbols.
- -fontdir <dir>: register font files in `<dir>` for PNG/JPEG rendering.

If you render PNG/JPEG in a headless environment, specify Java's headless mode at runtime as needed:

```
java -Djava.awt.headless=true -jar wmf2svg-[version].jar input.wmf output.png
```

To render with fonts that are not installed in the operating system, place `.ttf`, `.ttc`, `.otf`, `.pfa`, or `.pfb`
files in a directory and pass it before the input/output filenames:

```
java -Djava.awt.headless=true -jar wmf2svg-[version].jar -fontdir /path/to/fonts input.wmf output.png
```

It now requires Java 8.0 or later.

## Library usage

The parsers replay metafile records to a `Gdi` implementation. Choose the implementation that matches the output you
want.

### SVG output

```java
try (InputStream in = new FileInputStream("input.wmf");
		OutputStream out = new FileOutputStream("output.svg")) {
	SvgGdi gdi = new SvgGdi();
	new WmfParser().parse(in, gdi);
	gdi.write(out);
}
```

Use `new EmfParser()` for `.emf` input.

### PNG/JPEG output

```java
try (InputStream in = new FileInputStream("input.emf");
		OutputStream out = new FileOutputStream("output.png")) {
	AwtGdi gdi = new AwtGdi();
	gdi.setOpaqueBackground(true);
	new EmfParser().parse(in, gdi);
	gdi.write(out, "png");
}
```

`AwtGdi` renders through `Graphics2D` and writes raster images with `ImageIO`. Use `"jpeg"` to write JPEG.

### WMF/EMF output

`WmfGdi` and `EmfGdi` record GDI calls and serialize them as WMF or EMF. This can be used to generate metafiles
directly, or to transcode the supported subset of another metafile.

```java
try (InputStream in = new FileInputStream("input.wmf");
		OutputStream out = new FileOutputStream("output.emf")) {
	EmfGdi gdi = new EmfGdi();
	new WmfParser().parse(in, gdi);
	gdi.write(out);
}
```

```java
try (OutputStream out = new FileOutputStream("output.wmf")) {
	WmfGdi gdi = new WmfGdi();
	gdi.placeableHeader(0, 0, 1000, 1000, 1440);
	gdi.header();
	gdi.rectangle(100, 100, 900, 900);
	gdi.write(out);
}
```

Some advanced records are format-specific and may not be representable in the target metafile writer.

## Maven repository

```xml
<groupId>net.arnx</groupId>
<artifactId>wmf2svg</artifactId>
<version>0.10.4</version>
```

## Build

This project uses Maven to build a wmf2svg jar file.

```
mvn package
```

## History

- 2026-05-06: Fix many bugs.
- 2026-05-03: Fix many bugs.
- 2026-04-30: Improve EMF+ support. And add PNG/JPEG output through AWT/ImageIO and improve AWT PNG rendering fidelity.
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

<!--
- mvn verify
- git tag vX.XX.X && git push origin --tags
- revert: git tag -d vX.XX.X && git push origin :refs/tags/vX.XX.X
- mvn -Prelease clean deploy
-->
