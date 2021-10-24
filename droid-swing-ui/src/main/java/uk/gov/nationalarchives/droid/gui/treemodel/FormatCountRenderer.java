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

import javax.swing.Icon;
import javax.swing.JTable;
import javax.swing.SwingConstants;

import net.byteseek.swing.treetable.TreeTableModel;

/**
 * Renders the format count, using icons for 0, 1 or null, and a number for any more than that.
 *
 * @author rflitcroft
 *
 */
//TODO: why not inherit from DefaultCellRenderer?
public class FormatCountRenderer extends DefaultCellRenderer {

    private static final String ZERO_IDS = "format_count_small_ZERO.png";
    private static final String ONE_ID = "format_count_small_ONE.png";
    private static final String MULTIPLE_IDS = "format_count_small_MULTIPLE.png";

    private final Icon zeroIdentificationsIcon;
    private final Icon oneIdentificationIcon;
    private final Icon multipleIdentificationsIcon;

    /**
     * Creates a format count renderer and initialises the internal labels and icons
     * used to render the different counts.
     * @param treeTableModel The tree table model to use.
     * @param backColor the background color to render cells in.
     */
    public FormatCountRenderer(TreeTableModel treeTableModel, Color backColor) {
        super(treeTableModel, backColor, SwingConstants.CENTER);
        zeroIdentificationsIcon = getIconResource(ZERO_IDS);
        oneIdentificationIcon = getIconResource(ONE_ID);
        multipleIdentificationsIcon = getIconResource(MULTIPLE_IDS);
    }


    @Override
    public String getDisplayValue(final JTable table, final Object value,
                                  final boolean isSelected, final boolean hasFocus,
                                  final int row, final int column) {
        final Integer count = (Integer) value;
        return count != null && count > 1 ? "<html>(<u>" + count + "</u>)</html>" : null;
    }

    @Override
    public Icon getIcon(final JTable table, final Object value,
                        final boolean isSelected, final boolean hasFocus,
                        final int row, final int column) {
        final Icon icon;
        if (value == null) {
            icon = null;
        } else {
            switch((Integer) value) {
                case 0 : {
                    icon = zeroIdentificationsIcon;
                    break;
                }
                case 1: {
                    icon = oneIdentificationIcon;
                    break;
                }
                default: {
                    icon = multipleIdentificationsIcon;
                    break;
                }
            }
        }
        return icon;
    }
    
}
