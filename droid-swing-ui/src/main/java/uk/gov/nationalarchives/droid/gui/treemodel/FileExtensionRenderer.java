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
 * Renders file extensions, with a warning icon if there is file extension mismatch.
 *
 * @author a-mpalmer
 */
public class FileExtensionRenderer extends DefaultCellRenderer {

    private final Icon warningIcon;
    
    /**
     * Constructs a FileExtension renderer which supplies a warning icon on mismatches.
     *
     * @param treeTableModel the tree table model used by renderers to find node sin the tree.
     * @param backColor the background color to render in.
     */
    public FileExtensionRenderer(final TreeTableModel treeTableModel, final Color backColor) {
        super(treeTableModel, backColor, SwingConstants.LEFT);
        warningIcon = getIconResource("warning_extension_mismatch.gif");
    }
    
    @Override
    public Icon getIcon(final JTable table, final Object value,
                        final boolean isSelected, final boolean hasFocus,
                        final int row, final int column) {
        return getProfileNode() == null ? null : getProfileNode().getExtensionMismatch() ? warningIcon : null;
    }

}
