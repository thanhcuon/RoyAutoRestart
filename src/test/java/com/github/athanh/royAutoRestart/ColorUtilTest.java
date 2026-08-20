package com.github.athanh.royAutoRestart;

import com.github.athanh.royAutoRestart.utils.ColorUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ColorUtilTest {

    @Test
    public void testLegacyColorFormatting() {
        String input = "&cRed &aGreen &lBold &rReset";
        String output = ColorUtil.colorize(input);
        assertEquals("§cRed §aGreen §lBold §rReset", output);
    }

    @Test
    public void testHexColorFormatting() {
        String input = "&#FF5555Red Text &#00AA00Green";
        String output = ColorUtil.colorize(input);
        assertTrue(output.contains("§x§f§f§5§5§5§5Red Text"));
        assertTrue(output.contains("§x§0§0§a§a§0§0Green"));
    }

    @Test
    public void testTwoStopGradient() {
        String input = "<gradient:#FF0000:#00FF00>Hello World</gradient>";
        String output = ColorUtil.colorize(input);
        assertFalse(output.contains("<gradient"));
        assertFalse(output.contains("</gradient>"));
        assertTrue(output.contains("§x§f§f§0§0§0§0H"));
        assertTrue(output.contains("§x§0§0§f§f§0§0d"));
    }

    @Test
    public void testMultiStopGradientWithFormatting() {
        String input = "<gradient:#aaaaaa:#bbbbbb:#cccccc:#N>&lSERVER RESTART</gradient>";
        String output = ColorUtil.colorize(input);
        assertFalse(output.contains("<gradient"));
        assertFalse(output.contains("</gradient>"));
        assertTrue(output.contains("§lS"));
    }

    @Test
    public void testRainbowFormatting() {
        String input = "<rainbow>Rainbow Text</rainbow>";
        String output = ColorUtil.colorize(input);
        assertFalse(output.contains("<rainbow>"));
        assertFalse(output.contains("</rainbow>"));
        assertTrue(output.contains("§x"));
        assertTrue(output.contains("R"));
    }

    @Test
    public void testStripColor() {
        String input = "<gradient:#FF0000:#00FF00>&lHello &cWorld</gradient> &#FFAA00Test";
        String stripped = ColorUtil.stripColor(input);
        assertEquals("Hello World Test", stripped);
    }
}
