package net.arnx.wmf2svg.gdi.svg;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import net.arnx.wmf2svg.util.SymbolFontMappings;

public class SymbolFontMappingsTest {
	@Test
	public void testSymbolMapping() {
		assertTrue(SymbolFontMappings.isMappedFont("Symbol"));
		assertEquals("\u2200\u0391\u03B1", SymbolFontMappings.replace("Symbol", "\"Aa"));
	}

	@Test
	public void testWingdingsMapping() {
		assertTrue(SymbolFontMappings.isMappedFont("Wingdings"));
		assertEquals("\u2702", SymbolFontMappings.replace("Wingdings", "\""));
		assertEquals(new String(Character.toChars(0x1F589)), SymbolFontMappings.replace("Wingdings", "!"));
		assertEquals(new String(Character.toChars(0x1FA9F)), SymbolFontMappings.replace("Wingdings", "\u00FF"));
	}

	@Test
	public void testWingdingsNumberedFonts() {
		assertTrue(SymbolFontMappings.isMappedFont("Wingdings 2"));
		assertTrue(SymbolFontMappings.isMappedFont("Wingdings 3"));
		assertEquals(new String(Character.toChars(0x1F58A)), SymbolFontMappings.replace("Wingdings 2", "!"));
		assertEquals("\u2B60", SymbolFontMappings.replace("Wingdings 3", "!"));
	}

	@Test
	public void testMtExtraMapping() {
		assertTrue(SymbolFontMappings.isMappedFont("MT Extra"));
		assertEquals("\u0300\u2235\u21D5", SymbolFontMappings.replace("MT Extra", "#Qc"));
		assertEquals("!", SymbolFontMappings.replace("MT Extra", "!"));
	}
}
