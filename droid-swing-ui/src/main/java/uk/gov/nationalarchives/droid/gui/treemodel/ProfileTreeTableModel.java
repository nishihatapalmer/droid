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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableColumnModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;

import net.byteseek.swing.treetable.TreeTableModel;

import uk.gov.nationalarchives.droid.profile.NodeMetaData;
import uk.gov.nationalarchives.droid.profile.ProfileResourceNode;
import uk.gov.nationalarchives.droid.profile.referencedata.Format;

//TODO: sorting broken in DROID... need to see what's going on here.  Works in my treetable demo!

/**
 * A TreeTableModel which maps a ProfileResourceNode tree to a JTable.
 */
public class ProfileTreeTableModel extends TreeTableModel {

    private static final int DEFAULT_RESOURCE_COLUMN_WIDTH = 300;
    private static final int RESOURCE_COLUMN_INDEX = 0;
    private static final int EXTENSION_COLUMN_INDEX = 1;
    private static final int DATE_COLUMN_INDEX = 2;
    private static final int SIZE_COLUMN_INDEX = 3;
    private static final int ID_COUNT_COLUMN_INDEX = 4;
    private static final int FORMAT_COLUMN_INDEX = 5;
    private static final int VERSION_COLUMN_INDEX = 6;
    private static final int MIME_COLUMN_INDEX = 7;
    private static final int PUID_COLUMN_INDEX = 8;
    private static final int METHOD_COLUMN_INDEX = 9;
    private static final int HASH_COLUMN_INDEX = 10;

    private final Color backColor;

    /**
     * Constructs a ProfileTreeTableModel.
     * 
     * @param rootNode The root node of the tree.
     * @param backColor The background color cell renderers should use.
     */
    public ProfileTreeTableModel(final TreeNode rootNode, final Color backColor) {
        super(rootNode, false);
        this.backColor = backColor;
    }

    //CHECKSTYLE:OFF - cyclomatic complexity and too many returns.  This is still the clearest way to express the intent in my opinion.
    @Override
    public Object getColumnValue(final TreeNode treeNode, final int column) {
        final ProfileResourceNode node = (ProfileResourceNode) ((DefaultMutableTreeNode) treeNode).getUserObject();
        final NodeMetaData metadata = node.getMetaData();
        switch (column) {
            case RESOURCE_COLUMN_INDEX: return metadata.getName();
            case EXTENSION_COLUMN_INDEX: return metadata.getExtension();
            case DATE_COLUMN_INDEX: return metadata.getLastModifiedDate();
            case SIZE_COLUMN_INDEX: return metadata.getSize();
            case ID_COUNT_COLUMN_INDEX: return node.getIdentificationCount();
            case FORMAT_COLUMN_INDEX: return getNodeIdentificationDisplayText(node);
            case VERSION_COLUMN_INDEX: return getVersionIdentificationDisplayText(node);
            case MIME_COLUMN_INDEX: return getMimeIdentificationDisplayText(node);
            case PUID_COLUMN_INDEX: return getPUIDDisplayText(node);
            case METHOD_COLUMN_INDEX: return metadata.getIdentificationMethod();
            case HASH_COLUMN_INDEX: return metadata.getHash();
            default : return "";
        }
    }
    //CHECKSTYLE:ON

    @Override
    public TableColumnModel createTableColumnModel() {
        TableColumnModel columns = new DefaultTableColumnModel();
        columns.addColumn(createColumn(RESOURCE_COLUMN_INDEX, "Resource", DEFAULT_RESOURCE_COLUMN_WIDTH, new TreeRenderer(this, backColor)));
        columns.addColumn(createColumn(EXTENSION_COLUMN_INDEX, "Extension", new FileExtensionRenderer(this, backColor)));
        columns.addColumn(createColumn(DATE_COLUMN_INDEX, "Last modified", new DateRenderer(this, backColor)));
        columns.addColumn(createColumn(SIZE_COLUMN_INDEX, "Size", new FileSizeRenderer(this, backColor)));
        columns.addColumn(createColumn(ID_COUNT_COLUMN_INDEX, "Identifications", new FormatCountRenderer(this, backColor)));
        columns.addColumn(createColumn(FORMAT_COLUMN_INDEX, "Format", new DefaultCellRenderer(this, backColor, SwingConstants.LEFT)));
        columns.addColumn(createColumn(VERSION_COLUMN_INDEX, "Version", new DefaultCellRenderer(this, backColor, SwingConstants.CENTER)));
        columns.addColumn(createColumn(MIME_COLUMN_INDEX, "MIME type", new DefaultCellRenderer(this, backColor, SwingConstants.LEFT)));
        columns.addColumn(createColumn(PUID_COLUMN_INDEX, "PUID", new HyperlinkRenderer(this, backColor, SwingConstants.CENTER)));
        columns.addColumn(createColumn(METHOD_COLUMN_INDEX, "Method", new DefaultCellRenderer(this, backColor)));
        columns.addColumn(createColumn(HASH_COLUMN_INDEX, "Hash", new DefaultCellRenderer(this, backColor)));
        return columns;
    }

    //TODO: add comparators for relevant columns (e.g. case insensitive string comparisons).

    private String getNodeIdentificationDisplayText(ProfileResourceNode node) {
        return buildFormatInfo(node, Format::getName);
    }
    private String getVersionIdentificationDisplayText(ProfileResourceNode node) {
        return buildFormatInfo(node, Format::getVersion);
    }

    private String getMimeIdentificationDisplayText(ProfileResourceNode node) {
        return buildFormatInfo(node, Format::getMimeType);
    }

    private String getPUIDDisplayText(ProfileResourceNode node) {
        return buildFormatInfo(node, Format::getPuid);
    }

    private String buildFormatInfo(final ProfileResourceNode node, final Function<Format, String> stringProvider) {
        final String displayText;
        final List<Format> formatIdentifications = node.getFormatIdentifications();
        if (formatIdentifications.size() == 1) {
            displayText = stringProvider.apply(formatIdentifications.get(0));
        } else if (formatIdentifications.size() > 1) {
            final Set<String> formatInfo = new HashSet<>();
            for (Format f : formatIdentifications) {
                String name = stringProvider.apply(f);
                if (!name.isEmpty()) {
                    formatInfo.add(name);
                }
            }
            displayText = getMultiValuedString(formatInfo);
        } else {
            displayText = "";
        }
        return displayText;
    }

    private String getMultiValuedString(Set<String> values) {
        final StringBuilder builder = new StringBuilder();
        String separator = "";
        final String format = values.size() > 1 ? "%s\"%s\"" : "%s%s";
        for (String value : values) {
            builder.append(String.format(format, separator, value));
            separator = ", ";
        }
        return builder.toString();
    }


}
