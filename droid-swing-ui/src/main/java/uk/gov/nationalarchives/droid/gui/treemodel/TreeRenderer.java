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
import java.net.URL;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JTable;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;

import net.byteseek.swing.treetable.TreeTableCellRenderer;
import net.byteseek.swing.treetable.TreeTableModel;
import uk.gov.nationalarchives.droid.core.interfaces.NodeStatus;
import uk.gov.nationalarchives.droid.core.interfaces.ResourceType;
import uk.gov.nationalarchives.droid.profile.NodeMetaData;
import uk.gov.nationalarchives.droid.profile.ProfileResourceNode;

/**
 * Renders the tree node with indentations for level and expand/collapse handles.
 * <p>
 * Unlike the other cell renders, which are mostly subclassed from DefaultCellRenderer,
 * this renderer is sub-classed from the TreeTableCellRenderer provided with the tree table component.
 * It already handles mouse clicks, expansion/collapse and tree rendering.
 * <p>
 * All we do in this sub-class is to set the correct icon
 *
 * @author a-mpalmer
 *
 */
public class TreeRenderer extends TreeTableCellRenderer {

    private static final long serialVersionUID = -574370296768932560L;

    private final Icon folderResourceTypeIcon = getIconResource("folderResourceType");
    private final Icon containerResourceTypeIcon = getIconResource("containerResourceType");
    private final Icon fileResourceTypeIcon = getIconResource("fileResourceType");

    private final Icon folderResourceTypeNotDoneIcon = getIconResource("folderResourceType_NOT_DONE");
    private final Icon containerResourceTypeNotDoneIcon = getIconResource("containerResourceType_NOT_DONE");
    private final Icon fileResourceTypeNotDoneIcon = getIconResource("fileResourceType_NOT_DONE");

    private final Icon folderResourceTypeNotFoundIcon = getIconResource("folderResourceType_NOTFOUND");
    private final Icon containerResourceTypeNotFoundIcon = getIconResource("containerResourceType_NOTFOUND");
    private final Icon fileResourceTypeNotFoundIcon = getIconResource("fileResourceType_NOTFOUND");

    private final Icon folderResourceTypeEmptyIcon = getIconResource("folderResourceType_EMPTY");
    private final Icon folderResourceTypeAccessDeniedIcon = getIconResource("folderResourceType_ACCESSDENIED");
    private final Icon containerResourceTypeAccessDeniedIcon = getIconResource("containerResourceType_ACCESSDENIED");
    private final Icon fileResourceTypeAccessDeniedIcon = getIconResource("fileResourceType_ACCESSDENIED");

    private final Icon folderResourceTypeErrorIcon = getIconResource("folderResourceType_ERROR");
    private final Icon containerResourceTypeErrorIcon = getIconResource("containerResourceType_ERROR");
    private final Icon fileResourceTypeErrorIcon = getIconResource("fileResourceType_ERROR");

    private final Color evenBackColor;
    private final Color oddBackColor;

    /**
     * Constructs a TreeRenderer.
     *
     * @param treeTableModel the tree table model used by renderers to find nodes in the tree.
     * @param backColor the default background color for the node.
     */
    public TreeRenderer(final TreeTableModel treeTableModel, final Color backColor) {
        super(treeTableModel);
        this.evenBackColor = backColor;
        this.oddBackColor = ColorUtils.getContrastingColor(backColor);
    }

    @Override
    protected Color getUnselectedBackgroundColor(final JTable table, final Object value,
                                                final boolean hasFocus, final int row, final int column) {
        return (row % 2 == 0) ? evenBackColor : oddBackColor;
    }

    /**
     * Gets the correct icon for the node.
     *
     * @param node the node
     * @return the icon for the node
     */
    @Override
    protected Icon getNodeIcon(final TreeNode node) {
        Icon icon = null;
        final ProfileResourceNode profileNode = getProfileInfo(node);
        final NodeMetaData metadata = profileNode.getMetaData();
        if (metadata != null) {
            ResourceType nodeType = metadata.getResourceType();
            NodeStatus status = metadata.getNodeStatus();
            switch (nodeType) {
                case FOLDER: {
                    icon = getFolderIcon(profileNode, status);
                    break;
                }
                case FILE: {
                    icon = getFileIcon(profileNode, status);
                    break;
                }
                case CONTAINER: {
                    icon = getContainerIcon(profileNode, status);
                    break;
                }
                default:
            }
        }
        return icon;
    }

    @Override
    protected void setAdditionalProperties(final TreeNode treeNode, final JTable table, final Object value, final boolean isSelected,
                                           final boolean hasFocus, final int row, final int column) {
        setToolTipText(java.net.URLDecoder.decode(getProfileInfo(treeNode).getUri().toString()));
    }

    /**
     * @param profileNode The node to get the icon for.
     * @param status The node status
     * @return a file icon for the profile node with the status given.
     */
    protected Icon getFileIcon(ProfileResourceNode profileNode, NodeStatus status) {
        final Icon fileIcon;
        if (profileNode.getFilterStatus() != 1) {
            fileIcon = fileResourceTypeNotDoneIcon;
        } else {
            switch (status) {
                case NOT_DONE: {
                    fileIcon = fileResourceTypeNotDoneIcon;
                    break;
                }
                case ACCESS_DENIED: {
                    fileIcon = fileResourceTypeAccessDeniedIcon;
                    break;
                }
                case NOT_FOUND: {
                    fileIcon = fileResourceTypeNotFoundIcon;
                    break;
                }
                case ERROR: {
                    fileIcon = fileResourceTypeErrorIcon;
                    break;
                }
                default: {
                    fileIcon = fileResourceTypeIcon;
                    break;
                }
            }
        }
        return fileIcon;
    }

    /**
     * @param profileNode The node to get the icon for.
     * @param status The node status
     * @return a folder icon for the profile node with the status given.
     */
    protected Icon getFolderIcon(ProfileResourceNode profileNode, NodeStatus status) {
        Icon folderIcon;
        if (profileNode.getFilterStatus() != 1) {
            folderIcon = folderResourceTypeNotDoneIcon;
        } else {
            switch (status) {
                case NOT_DONE: {
                    folderIcon = folderResourceTypeNotDoneIcon;
                    break;
                }
                case ACCESS_DENIED: {
                    folderIcon = folderResourceTypeAccessDeniedIcon;
                    break;
                }
                case EMPTY: {
                    folderIcon = folderResourceTypeEmptyIcon;
                    break;
                }
                case NOT_FOUND: {
                    folderIcon = folderResourceTypeNotFoundIcon;
                    break;
                }
                case ERROR: {
                    folderIcon = folderResourceTypeErrorIcon;
                    break;
                }
                default: {
                    folderIcon = folderResourceTypeIcon;
                    break;
                }
            }
        }
        return folderIcon;
    }

    /**
     * @param profileNode The node to get the icon for.
     * @param status The node status
     * @return a container icon for the profile node with the status given.
     */
    protected Icon getContainerIcon(ProfileResourceNode profileNode, NodeStatus status) {
        final Icon containerIcon;
        if (profileNode.getFilterStatus() != 1) {
            containerIcon = containerResourceTypeNotDoneIcon;
        } else {
            switch (status) {
                case NOT_DONE: // should be impossible, but implemented anyway.
                    containerIcon = containerResourceTypeNotDoneIcon;
                    break;
                case ACCESS_DENIED:
                    containerIcon = containerResourceTypeAccessDeniedIcon;
                    break;
                case NOT_FOUND:
                    containerIcon = containerResourceTypeNotFoundIcon;
                    break;
                case ERROR:
                    containerIcon = containerResourceTypeErrorIcon;
                    break;
                default:
                    containerIcon = containerResourceTypeIcon;
            }
        }
        return containerIcon;
    }

    private Icon getIconResource(final String resourceName) {
        final String resourcePath = String.format("uk/gov/nationalarchives/droid/icons/%s.gif", resourceName);
        final URL imgURL = getClass().getClassLoader().getResource(resourcePath);
        return imgURL == null ? null : new ImageIcon(imgURL);
    }

    private ProfileResourceNode getProfileInfo(final TreeNode node) {
        return (ProfileResourceNode) ((DefaultMutableTreeNode) node).getUserObject();
    }

}
