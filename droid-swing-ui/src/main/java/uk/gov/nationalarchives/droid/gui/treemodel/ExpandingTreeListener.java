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

import java.awt.Cursor;
import java.util.Enumeration;
import java.util.List;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;

import net.byteseek.swing.treetable.TreeTableModel;
import uk.gov.nationalarchives.droid.gui.ProfileForm;
import uk.gov.nationalarchives.droid.profile.ProfileManager;
import uk.gov.nationalarchives.droid.profile.ProfileResourceNode;

/**
 * @author rflitcroft, mpalmer
 *
 */
public class ExpandingTreeListener implements TreeTableModel.ExpandCollapseListener {

    private ProfileManager profileManager;
    private ProfileForm profileForm;

    /**
     * @param profileManager a profile manager. 
     * @param profileForm the parent profile form 
     */
    public ExpandingTreeListener(ProfileManager profileManager, ProfileForm profileForm) {
        this.profileManager = profileManager;
        this.profileForm = profileForm;
    }

    @Override
    public boolean nodeExpanding(final TreeNode treeNode) {
        final DefaultMutableTreeNode expandingNode = (DefaultMutableTreeNode) treeNode;
        final ProfileResourceNode prn = (ProfileResourceNode) expandingNode.getUserObject();
        profileForm.getInMemoryNodes().put(prn.getId(), expandingNode);
        // If we don't already have children, fetch them from the database:
        if (expandingNode.getChildCount() == 0) {
            final List<ProfileResourceNode> childNodes = loadChildNodes(prn);
            if (childNodes.isEmpty()) { // If no children in database, flag the node doesn't allow children.
                expandingNode.setAllowsChildren(false);
            } else { // build the child nodes for the tree.
                expandingNode.setAllowsChildren(true);
                for (ProfileResourceNode node : childNodes) {
                    final DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(node, node.allowsChildren());
                    expandingNode.add(newNode);
                    profileForm.getInMemoryNodes().put(node.getId(), newNode);
                }
            }
        }
        return true;
    }

    private List<ProfileResourceNode> loadChildNodes(ProfileResourceNode prn) {
        try {
            profileForm.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            return profileManager.findProfileResourceNodeAndImmediateChildren(
                    profileForm.getProfile().getUuid(), prn.getId());
        } finally {
            profileForm.setCursor(Cursor.getDefaultCursor());
        }
    }

    @Override
    public boolean nodeCollapsing(TreeNode treeNode) {
        // Do nothing on node collapse - retain all previously expanded children in memory.
        // There will be a slight increase in memory (a few hundred or thousand nodes possibly),
        // but loading nodes will be much faster than going to the database, and it also preserves
        // any child expansion structure in the tree model, so if you re-expand a parent, all the sub-trees
        // you were previously exploring are as they were instead of being reset on each collapse.
        return true;
    }
}
