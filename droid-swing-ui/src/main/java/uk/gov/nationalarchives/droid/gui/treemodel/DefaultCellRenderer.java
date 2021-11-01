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
import java.awt.Component;
import java.net.URL;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.TableCellRenderer;
import javax.swing.tree.DefaultMutableTreeNode;

import net.byteseek.swing.treetable.TreeTableModel;

import uk.gov.nationalarchives.droid.profile.ProfileResourceNode;

/**
 * Default cell renderer for the DROID tree.  Takes care of alternating row background colors, and filtering out
 * values if the node is filtered.  Has some utility methods for more specialised renderers to override,
 * such as getDisplayValue() or to use such as getImageIcon().
 *
 * @author a-mpalmer
 */
public class DefaultCellRenderer implements TableCellRenderer {

    /**
     * The JLabel used as a renderer.
     */
    private final JLabel renderer = new JLabel();

    /**
     * The lighter back color used in alternating rows in the table.
     */
    private final Color evenRowColor;

    /**
     * The darker back color used in alternating rows in the table.
     */
    private final Color oddRowColor;

    /**
     * The TreeTableModel which can be used to get DefaultMutableNodes and ProfileResourceNodes from the tree
     * given its table row index in getTableCellRendererComponent().
     */
    private final TreeTableModel treeTableModel;

    /**
     * The current tree node being rendered, or null.
     */
    private DefaultMutableTreeNode treeNode;

    /**
     * The current profile resource node being rendered, or null.
     */
    private ProfileResourceNode profileResourceNode;


    /**
     * @param treeTableModel the tree table model used by renderers to find nodes in the tree.
     * @param backColor The default background color to render in
     */
    public DefaultCellRenderer(final TreeTableModel treeTableModel, final Color backColor) {
        renderer.setOpaque(true);
        renderer.setHorizontalAlignment(SwingConstants.CENTER);
        this.evenRowColor = backColor;
        this.oddRowColor = ColorUtils.getContrastingColor(backColor);
        this.treeTableModel = treeTableModel;
    }

    /**
     * Overloaded constructor for the default cell renderer taking an alignment.
     *
     * @param treeTableModel the tree table model used by renderers to find nodes in the tree.
     * @param backColor - the default background color for the cell
     * @param alignment - the alignment (SwingConstants) of the cell contents.
     */
    public DefaultCellRenderer(final TreeTableModel treeTableModel, final Color backColor, final int alignment) {
        this(treeTableModel, backColor);
        renderer.setHorizontalAlignment(alignment);
    }

    @Override
    public Component getTableCellRendererComponent(final JTable table, final Object value,
                                                   final boolean isSelected, final boolean hasFocus,
                                                   final int row, final int column) {
        setNodes(row);
        if (isSelected) {
            renderer.setBackground(getSelectedBackgroundColor(table, value, hasFocus, row, column));
            renderer.setForeground(getSelectedForegroundColor(table, value, hasFocus, row, column));
        } else {
            renderer.setBackground(getUnselectedBackgroundColor(table, value, hasFocus, row, column));
            renderer.setForeground(getUnselectedForegroundColor(table, value, hasFocus, row, column));
        }
        if (isFiltered()) {
            renderer.setText("");
            renderer.setToolTipText("");
            renderer.setIcon(null);
        } else {
            String displayValue = getDisplayValue(table, value, isSelected, hasFocus, row, column);
            renderer.setText(displayValue);
            renderer.setToolTipText(displayValue);
            renderer.setIcon(getIcon(table, value, isSelected, hasFocus, row, column));
        }
        return renderer;
    }

    /**
     * @param table the table
     * @param value value of the current cell
     * @param hasFocus if it has focus
     * @param row the row in the table
     * @param column the column in the table.
     * @return Returns the selected background color to use.
     */
    public Color getSelectedBackgroundColor(final JTable table, final Object value, final boolean hasFocus,
                                            final int row, final int column) {
        return table.getSelectionBackground();
    }

    /**
     * @param table the table
     * @param value value of the current cell
     * @param hasFocus if it has focus
     * @param row the row in the table
     * @param column the column in the table.
     * @return Returns the selected foreground color to use.
     */
    public Color getSelectedForegroundColor(final JTable table, final Object value, final boolean hasFocus,
                                            final int row, final int column) {
        return table.getSelectionForeground();
    }

    /**
     * @param table the table
     * @param value value of the current cell
     * @param hasFocus if it has focus
     * @param row the row in the table
     * @param column the column in the table.
     * @return Returns the unselected background color to use.
     */
    public Color getUnselectedBackgroundColor(final JTable table, final Object value, final boolean hasFocus,
                                              final int row, final int column) {
        return row % 2 == 0 ? evenRowColor : oddRowColor;
    }

    /**
     * @param table the table
     * @param value value of the current cell
     * @param hasFocus if it has focus
     * @param row the row in the table
     * @param column the column in the table.
     * @return Returns the unselected foreground color to use.
     */
    public Color getUnselectedForegroundColor(final JTable table, final Object value, final boolean hasFocus,
                                             final int row, final int column) {
        return table.getForeground();
    }

    /**
     * getDisplayValue returns the display value for an object in the renderer.
     * This can be overridden by subclasses to provide for more specialised display values (e.g. the HyperLinkRenderer).
     *
     * @param table the table
     * @param value value of the current cell
     * @param isSelected if it is selected
     * @param hasFocus if it has focus
     * @param row the row in the table
     * @param column the column in the table.
     * @return String the display value of the object.
     */
    public String getDisplayValue(final JTable table, final Object value,
                                  final boolean isSelected, final boolean hasFocus,
                                  final int row, final int column) {
        return value == null ? "" : value.toString();
    }
    
    /**
     * Returns an Icon for the tree node being processed in getTableCellRendererComponent().
     *
     * By default, no icon is returned for the node to render.  Return an icon by overriding in a subclass renderer.
     * @param table the table
     * @param value value of the current cell
     * @param isSelected if it is selected
     * @param hasFocus if it has focus
     * @param row the row in the table
     * @param column the column in the table
     * @return An icon to render.
     */
    public Icon getIcon(final JTable table, final Object value,
                        final boolean isSelected, final boolean hasFocus,
                        final int row, final int column) {
        return null;
    }

    /**
     * @return the profile reosource node being rendered.
     */
    protected ProfileResourceNode getProfileNode() {
        return profileResourceNode;
    }

    /**
     * @return the tree table model associated with this renderer.
     */
    protected TreeTableModel getTreeTableModel() {
        return treeTableModel;
    }

    /**
     * Returns the filtered status of the current profile node.
     * If there is no current profile node, then it will return false - as by definition, if there is no node,
     * it cannot be a thing which has been filtered.
     *
     * @return the filter status of the current node, or false if there is no current node.
     */
    protected  boolean isFiltered() {
        return profileResourceNode != null && profileResourceNode.getFilterStatus() != 1;
    }

    /**
     * Sets the DefaultMutableTreeNode and associated ProfileResourceNode for the given row in the table,
     * as fields in this class as the first action in the getTableCellRendererComponent() method.
     * If there is no node for that row, both tree node and profile node will be set to null.
     *
     * @param row The row in the table to set the tree and profile nodes for.
     */
    protected void setNodes(final int row) {
        treeNode = (DefaultMutableTreeNode) treeTableModel.getNodeAtTableRow(row);
        profileResourceNode = treeNode == null ? null : (ProfileResourceNode) treeNode.getUserObject();
    }

    /**
     * General utility method for sub-classes to obtain an icon given the resource name.
     *
     * @param resourceName the name of the icon resource to load.
     * @return An icon containing the loaded icon resource.
     */
    protected Icon getIconResource(String resourceName) {
        String resourcePath = String.format("uk/gov/nationalarchives/droid/icons/%s", resourceName);
        URL imgURL = getClass().getClassLoader().getResource(resourcePath);
        return imgURL == null ? null : new ImageIcon(imgURL);
    }
   
}
