/*
 * Copyright (c) 2016, The National Archives <pronom@nationalarchives.gov.uk>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following
 * conditions are met:
 *
 *  * Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 *  * Neither the name of the The National Archives nor the
 *    names of its contributors may be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package uk.gov.nationalarchives.droid.gui.treemodel;

import java.awt.Color;

/**
 * Static utility class with some methods to get different shades of Color.
 *
 * @author a-mpalmer
 */
public final class ColorUtils {

    private static final double LIGHTER_SCALE = 1.07;
    private static final double DARKER_SCALE = 0.95;
    private static final int MINVALUE = 0;
    private static final int MAXVALUE = 255;
    
    private ColorUtils() {
    }
  
    
    private static int getScaledColorComponent(int colorComponent, double scaleFactor) {
        int scaledValue = (int) (colorComponent * scaleFactor);
        if (scaledValue < MINVALUE) {
            scaledValue = MINVALUE;
        } else if (scaledValue > MAXVALUE) {
            scaledValue = MAXVALUE;
        }
        return scaledValue;
    }
    
    /**
     * 
     * @param color The color to adjust
     * @param scaleFactor the amount to scale the colors by
     * @return The adjusted color
     */
    public static Color getScaledColor(Color color, double scaleFactor) {
        return new Color(
                getScaledColorComponent(color.getRed(), scaleFactor),
                getScaledColorComponent(color.getGreen(), scaleFactor),
                getScaledColorComponent(color.getBlue(), scaleFactor));
    }

    /**
     * @param color The color to get a contrasting color for.
     * @return A color which is scaled either a bit lighter if the color is mostly dark, or scaled a bit darker.
     */
    public static Color getContrastingColor(Color color) {
        return colorIsDark(color) ? getLighterColor(color) : getDarkerColor(color);
    }

    private static boolean colorIsDark(Color color) {
        final int halfBrightness = 128;
        return color.getRed() < halfBrightness && color.getBlue() < halfBrightness && color.getGreen() < halfBrightness;
    }

    /**
     * 
     * @param color The color to make darker
     * @return The darker color
     */
    public static Color getDarkerColor(Color color) {
        return getScaledColor(color, DARKER_SCALE);
    }
    
    /**
     * 
     * @param color The color to make lighter
     * @return The lighter color
     */
    public static Color getLighterColor(Color color) {
        return getScaledColor(color, LIGHTER_SCALE);
    }

}
