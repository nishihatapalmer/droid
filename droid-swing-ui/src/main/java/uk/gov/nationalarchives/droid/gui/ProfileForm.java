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
package uk.gov.nationalarchives.droid.gui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDropEvent;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import java.util.function.Predicate;

import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JSlider;
import javax.swing.ListSelectionModel;
import javax.swing.TransferHandler;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;

import org.apache.commons.lang.StringUtils;

import uk.gov.nationalarchives.droid.core.interfaces.ResourceType;
import uk.gov.nationalarchives.droid.core.interfaces.config.DroidGlobalProperty;
import uk.gov.nationalarchives.droid.core.interfaces.filter.Filter;
import uk.gov.nationalarchives.droid.gui.action.CloseProfileAction;
import uk.gov.nationalarchives.droid.gui.action.OpenContainingFolderAction;
import uk.gov.nationalarchives.droid.gui.action.SaveProfileWorker;
import uk.gov.nationalarchives.droid.gui.treemodel.ExpandingTreeListener;
import uk.gov.nationalarchives.droid.gui.treemodel.ProfileTreeTableModel;
import uk.gov.nationalarchives.droid.gui.treemodel.ColorUtils;
import uk.gov.nationalarchives.droid.gui.widgetwrapper.FileChooserProxy;
import uk.gov.nationalarchives.droid.gui.widgetwrapper.FileChooserProxyImpl;
import uk.gov.nationalarchives.droid.gui.widgetwrapper.JOptionPaneProxy;
import uk.gov.nationalarchives.droid.gui.worker.DroidJob;
import uk.gov.nationalarchives.droid.profile.NodeMetaData;
import uk.gov.nationalarchives.droid.profile.ProfileEventListener;
import uk.gov.nationalarchives.droid.profile.ProfileInstance;
import uk.gov.nationalarchives.droid.profile.ProfileManager;
import uk.gov.nationalarchives.droid.profile.ProfileResourceNode;
import uk.gov.nationalarchives.droid.profile.ProfileState;

import net.byteseek.swing.treetable.TreeTableModel;
import net.byteseek.swing.treetable.TreeUtils;

/**
 * 
 * @author rflitcroft
 */
public class ProfileForm extends JPanel {

    /** */
    private static final int MAX_LEVELS_TO_EXPAND = 3;

    private static final long serialVersionUID = 1671584434169040994L;
    private static final int ROW_HEIGHT = 28; // height of rows in the tree table.
    private static final Predicate<TreeNode> TREE_NODE_ALLOWS_CHILDREN = node -> node.getParent() != null && node.getAllowsChildren();
    private static final String PUID_VALUE_PREFIX = "<html><a href=\"\">";
    private static final String PUID_VALUE_SUFFIX = "</a></html>";
    private static final String PUID_COLUMN_NAME = "PUID";
    private static final String IDENTIFICATIONS_COLUMN_NAME = "Identifications";

    /**
     * Groups folders in the tree before files.   Groups are sorted independently of each other.
     */
    //CHECKSTYLE:OFF - seems to think that type1 and type2 requires JavaDoc!!!
    private static final Comparator<TreeNode> FOLDER_GROUPING_COMPARATOR = (o1, o2) -> {
        final ResourceType type1 = getResourceType(o1);
        final ResourceType type2 = getResourceType(o2);
        return type1 == type2 ? 0 : type1 == ResourceType.FOLDER ? -1 : type2 == ResourceType.FOLDER ? 1 : 0;
    };
    //CHECKSTYLE:ON

    /**
     * Defines a predicate that filters a node out of the tree on the ProfileResourceNode.getFilterStatus().
     */
    private static final Predicate<TreeNode> FILTER_BY_STATUS =
            treeNode -> treeNode.getParent() != null && getProfileResourceNode(treeNode).getFilterStatus() == 0;

    private DefaultTreeModel treeModel;
    private TreeTableModel treeTableModel;
    private ProfileInstance profile;
    private DroidMainFrame droidMainUi;
    private DroidUIContext context;
    private ProfileEventListener listener;
    private ProfileTabComponent profileTab;
    private DroidJob job;
    private MultiIdentificationDialog multiIdentificationDialog;
    private Map<Long, DefaultMutableTreeNode> inMemoryNodes = new HashMap<>();

    /**
     * 
     * @param droidMainUi the droid ui frame
     * @param context the droid ui context
     * @param listener a profile event listener
     */
    public ProfileForm(DroidMainFrame droidMainUi, DroidUIContext context, ProfileEventListener listener) {
        this.droidMainUi = droidMainUi;
        this.context = context;
        this.listener = listener;
        initComponents();
        profileTab = new ProfileTabComponent(this);
        multiIdentificationDialog = new MultiIdentificationDialog(this);
        initOutline();
    }

    /**
     * 
     * @param droidMainUi the droidf main ui frame
     * @param context the droid ui context
     * @param profile a profile instance
     * @param listener a profile event listener
     */
    public ProfileForm(DroidMainFrame droidMainUi, DroidUIContext context, ProfileInstance profile,
            ProfileEventListener listener) {
        this(droidMainUi, context, listener);
        this.profile = profile;
    }

    private void initOutline() {
        // Set up the table
        new Date();
        final Color backColor = jTable1.getBackground();
        jTable1.setShowHorizontalLines(false);
        jTable1.setShowVerticalLines(true);
        jTable1.setGridColor(ColorUtils.getDarkerColor(backColor));
        jTable1.setRowHeight(ROW_HEIGHT);
        final TreeMouseAdapter mouseAdapter = new TreeMouseAdapter();
        jTable1.addMouseListener(mouseAdapter);
        jTable1.addMouseMotionListener(mouseAdapter);

        // Set up the tree and table models
        final DefaultMutableTreeNode root = new DefaultMutableTreeNode(null, true);
        treeModel = new DefaultTreeModel(root, true);
        treeTableModel = new ProfileTreeTableModel(root, backColor);
        treeModel.addTreeModelListener(treeTableModel);
        treeTableModel.addExpandCollapseListener(new ExpandingTreeListener(droidMainUi.getProfileManager(), this));
        treeTableModel.setGroupingComparator(FOLDER_GROUPING_COMPARATOR);
        treeTableModel.bindTable(jTable1);

        // Set up file dropping to add new files or folders:
        setDropFilesOn(jTable1);
        setDropFilesOn(jScrollPane2);
    }

    private void setDropFilesOn(JComponent component) {
        // Support dropping files on to the results outline.
        component.setDropTarget(new DropTarget() {
            public synchronized void drop(DropTargetDropEvent evt) {
                if (profile.getState() == ProfileState.VIRGIN) {
                    if (acceptDrop(evt)) {
                        evt.acceptDrop(DnDConstants.ACTION_COPY);
                        try {
                            final List<File> droppedFiles = (List<File>)
                                    evt.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                            droidMainUi.addFilesAndFolders(droppedFiles);
                        } catch (UnsupportedFlavorException e) {
                            // Ignore - the worst that can happen is we don't get to drop files.
                        } catch (IOException e) {
                            // Ignore - the worst that can happen is we don't get to drop files.
                        }
                        evt.dropComplete(true);
                    }
                }
            }
        });
    }

    private boolean acceptDrop(DropTargetDropEvent evt) {
        boolean acceptDrop = false;
        for (DataFlavor flavour : evt.getCurrentDataFlavors()) {
            if (flavour.equals(DataFlavor.javaFileListFlavor)) {
                acceptDrop = true;
                break;
            }
        }
        return acceptDrop;
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed"
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPopupMenu1 = new javax.swing.JPopupMenu();
        PopupAddFilesAndFolders = new javax.swing.JMenuItem();
        PopupRemoveFilesOrFolders = new javax.swing.JMenuItem();
        PopupSeparator1 = new javax.swing.JSeparator();
        PopupOpenContainingFolder = new javax.swing.JMenuItem();
        PopupMenuCopyToClipboard = new javax.swing.JMenuItem();
        jSeparator1 = new javax.swing.JSeparator();
        PopupMenuExpandChildren = new javax.swing.JMenuItem();
        PopupMenuExpandNextThree = new javax.swing.JMenuItem();
        jPanel3 = new javax.swing.JPanel();
        statusProgressPanel = new javax.swing.JPanel();
        statusLabel = new javax.swing.JLabel();
        statusProgressBar = new javax.swing.JProgressBar();
        throttlePanel = new javax.swing.JPanel();
        throttleSlider = new javax.swing.JSlider();
        throttleLabel = new javax.swing.JLabel();
        progressPanel = new javax.swing.JPanel();
        profileProgressBar = new javax.swing.JProgressBar();
        jScrollPane2 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();

        jPopupMenu1.addPopupMenuListener(new javax.swing.event.PopupMenuListener() {
            public void popupMenuCanceled(javax.swing.event.PopupMenuEvent evt) {
            }
            public void popupMenuWillBecomeInvisible(javax.swing.event.PopupMenuEvent evt) {
            }
            public void popupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent evt) {
                jPopupMenu1PopupMenuWillBecomeVisible(evt);
            }
        });
        jPopupMenu1.addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentShown(java.awt.event.ComponentEvent evt) {
                jPopupMenu1ComponentShown(evt);
            }
        });

        PopupAddFilesAndFolders.setIcon(new javax.swing.ImageIcon(getClass().getResource("/uk/gov/nationalarchives/droid/OldIcons/Add small.png"))); // NOI18N
        PopupAddFilesAndFolders.setText(org.openide.util.NbBundle.getMessage(ProfileForm.class, "ProfileForm.PopupAddFilesAndFolders.text")); // NOI18N
        PopupAddFilesAndFolders.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                PopupAddFilesAndFoldersActionPerformed(evt);
            }
        });
        jPopupMenu1.add(PopupAddFilesAndFolders);

        PopupRemoveFilesOrFolders.setIcon(new javax.swing.ImageIcon(getClass().getResource("/uk/gov/nationalarchives/droid/OldIcons/Remove small.png"))); // NOI18N
        PopupRemoveFilesOrFolders.setText(org.openide.util.NbBundle.getMessage(ProfileForm.class, "ProfileForm.PopupRemoveFilesOrFolders.text")); // NOI18N
        PopupRemoveFilesOrFolders.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                PopupRemoveFilesOrFoldersActionPerformed(evt);
            }
        });
        jPopupMenu1.add(PopupRemoveFilesOrFolders);
        jPopupMenu1.add(PopupSeparator1);

        PopupOpenContainingFolder.setIcon(new javax.swing.ImageIcon(getClass().getResource("/uk/gov/nationalarchives/droid/icons/Icon_External_Link.png"))); // NOI18N
        PopupOpenContainingFolder.setText(org.openide.util.NbBundle.getMessage(ProfileForm.class, "ProfileForm.PopupOpenContainingFolder.text")); // NOI18N
        PopupOpenContainingFolder.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                PopupOpenContainingFolderActionPerformed(evt);
            }
        });
        jPopupMenu1.add(PopupOpenContainingFolder);

        PopupMenuCopyToClipboard.setIcon(new javax.swing.ImageIcon(getClass().getResource("/uk/gov/nationalarchives/droid/OldIcons/Clipboard small.png"))); // NOI18N
        PopupMenuCopyToClipboard.setText(org.openide.util.NbBundle.getMessage(ProfileForm.class, "ProfileForm.PopupMenuCopyToClipboard.text")); // NOI18N
        PopupMenuCopyToClipboard.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                PopupMenuCopyToClipboardActionPerformed(evt);
            }
        });
        jPopupMenu1.add(PopupMenuCopyToClipboard);
        jPopupMenu1.add(jSeparator1);

        PopupMenuExpandChildren.setText(org.openide.util.NbBundle.getMessage(ProfileForm.class, "ProfileForm.PopupMenuExpandChildren.text")); // NOI18N
        PopupMenuExpandChildren.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                PopupMenuExpandChildrenActionPerformed(evt);
            }
        });
        jPopupMenu1.add(PopupMenuExpandChildren);

        PopupMenuExpandNextThree.setText(org.openide.util.NbBundle.getMessage(ProfileForm.class, "ProfileForm.PopupMenuExpandNextThree.text")); // NOI18N
        PopupMenuExpandNextThree.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                PopupMenuExpandNextThreeActionPerformed(evt);
            }
        });
        jPopupMenu1.add(PopupMenuExpandNextThree);

        statusLabel.setLabelFor(statusProgressBar);
        statusLabel.setText(org.openide.util.NbBundle.getMessage(ProfileForm.class, "ProfileForm.statusLabel.text")); // NOI18N

        javax.swing.GroupLayout statusProgressPanelLayout = new javax.swing.GroupLayout(statusProgressPanel);
        statusProgressPanel.setLayout(statusProgressPanelLayout);
        statusProgressPanelLayout.setHorizontalGroup(
            statusProgressPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(statusProgressPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(statusLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 94, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(statusProgressBar, javax.swing.GroupLayout.PREFERRED_SIZE, 125, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        statusProgressPanelLayout.setVerticalGroup(
            statusProgressPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, statusProgressPanelLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(statusProgressPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(statusProgressBar, javax.swing.GroupLayout.PREFERRED_SIZE, 14, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(statusLabel))
                .addContainerGap())
        );

        throttlePanel.setVisible(false);

        throttleSlider.setForeground(new java.awt.Color(245, 246, 247));
        throttleSlider.setMaximum(1000);
        throttleSlider.setMinorTickSpacing(100);
        throttleSlider.setPaintLabels(true);
        throttleSlider.setPaintTicks(true);
        throttleSlider.setToolTipText(org.openide.util.NbBundle.getMessage(ProfileForm.class, "ProfileForm.throttleSlider.toolTipText")); // NOI18N
        throttleSlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                throttleSliderStateChanged(evt);
            }
        });

        throttleLabel.setText(org.openide.util.NbBundle.getMessage(ProfileForm.class, "ProfileForm.throttleLabel.text")); // NOI18N

        javax.swing.GroupLayout throttlePanelLayout = new javax.swing.GroupLayout(throttlePanel);
        throttlePanel.setLayout(throttlePanelLayout);
        throttlePanelLayout.setHorizontalGroup(
            throttlePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(throttlePanelLayout.createSequentialGroup()
                .addComponent(throttleLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(throttleSlider, javax.swing.GroupLayout.DEFAULT_SIZE, 119, Short.MAX_VALUE)
                .addContainerGap())
        );
        throttlePanelLayout.setVerticalGroup(
            throttlePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(throttlePanelLayout.createSequentialGroup()
                .addGroup(throttlePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(throttleSlider, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, 0, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(throttlePanelLayout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(throttleLabel)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        profileProgressBar.setToolTipText(org.openide.util.NbBundle.getMessage(ProfileForm.class, "ProfileForm.profileProgressBar.toolTipText")); // NOI18N
        profileProgressBar.setString(org.openide.util.NbBundle.getMessage(ProfileForm.class, "ProfileForm.profileProgressBar.string")); // NOI18N
        profileProgressBar.setStringPainted(true);

        javax.swing.GroupLayout progressPanelLayout = new javax.swing.GroupLayout(progressPanel);
        progressPanel.setLayout(progressPanelLayout);
        progressPanelLayout.setHorizontalGroup(
            progressPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(progressPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(profileProgressBar, javax.swing.GroupLayout.DEFAULT_SIZE, 386, Short.MAX_VALUE))
        );
        progressPanelLayout.setVerticalGroup(
            progressPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(progressPanelLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(profileProgressBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                .addComponent(progressPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(statusProgressPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(throttlePanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(throttlePanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(progressPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(statusProgressPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        jScrollPane2.setViewportView(jTable1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jScrollPane2)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 634, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void PopupOpenContainingFolderActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_PopupOpenContainingFolderActionPerformed
        openSelectedFolders();
    }//GEN-LAST:event_PopupOpenContainingFolderActionPerformed

    private void PopupAddFilesAndFoldersActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_PopupAddFilesAndFoldersActionPerformed
        droidMainUi.jButtonAddFileActionPerformed(evt);
    }//GEN-LAST:event_PopupAddFilesAndFoldersActionPerformed

    private void PopupRemoveFilesOrFoldersActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_PopupRemoveFilesOrFoldersActionPerformed
        droidMainUi.jButtonRemoveFilesAndFolderActionPerformed(evt);
    }//GEN-LAST:event_PopupRemoveFilesOrFoldersActionPerformed

    private void jPopupMenu1ComponentShown(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_jPopupMenu1ComponentShown
        PopupAddFilesAndFolders.setEnabled(droidMainUi.getAddEnabled());
        PopupRemoveFilesOrFolders.setEnabled(droidMainUi.getRemoveEnabled());
    }//GEN-LAST:event_jPopupMenu1ComponentShown

    private void jPopupMenu1PopupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent evt) {//GEN-FIRST:event_jPopupMenu1PopupMenuWillBecomeVisible
        PopupAddFilesAndFolders.setEnabled(droidMainUi.getAddEnabled());
        PopupRemoveFilesOrFolders.setEnabled(droidMainUi.getRemoveEnabled());
        PopupOpenContainingFolder.setEnabled(anyRowsSelected());
        PopupMenuCopyToClipboard.setEnabled(anyRowsSelected());
    }//GEN-LAST:event_jPopupMenu1PopupMenuWillBecomeVisible

    private void PopupMenuCopyToClipboardActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_PopupMenuCopyToClipboardActionPerformed
        copySelectedToClipboard();
    }//GEN-LAST:event_PopupMenuCopyToClipboardActionPerformed

    private void PopupMenuExpandChildrenActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_PopupMenuExpandChildrenActionPerformed
       expandSelectedNodes(false);
    }//GEN-LAST:event_PopupMenuExpandChildrenActionPerformed

    private void PopupMenuExpandNextThreeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_PopupMenuExpandNextThreeActionPerformed
        expandSelectedNodes(true);
    }//GEN-LAST:event_PopupMenuExpandNextThreeActionPerformed

    private void throttleSliderStateChanged(javax.swing.event.ChangeEvent evt) {// GEN-FIRST:event_throttleSliderStateChanged
        throttleLabel.setText(String.format("Throttle: %s ms", throttleSlider.getValue()));
        context.getProfileManager().setThrottleValue(profile.getUuid(), throttleSlider.getValue());

    }// GEN-LAST:event_throttleSliderStateChanged

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JMenuItem PopupAddFilesAndFolders;
    private javax.swing.JMenuItem PopupMenuCopyToClipboard;
    private javax.swing.JMenuItem PopupMenuExpandChildren;
    private javax.swing.JMenuItem PopupMenuExpandNextThree;
    private javax.swing.JMenuItem PopupOpenContainingFolder;
    private javax.swing.JMenuItem PopupRemoveFilesOrFolders;
    private javax.swing.JSeparator PopupSeparator1;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPopupMenu jPopupMenu1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JTable jTable1;
    private javax.swing.JProgressBar profileProgressBar;
    private javax.swing.JPanel progressPanel;
    private javax.swing.JLabel statusLabel;
    private javax.swing.JProgressBar statusProgressBar;
    private javax.swing.JPanel statusProgressPanel;
    private javax.swing.JLabel throttleLabel;
    private javax.swing.JPanel throttlePanel;
    private javax.swing.JSlider throttleSlider;
    // End of variables declaration//GEN-END:variables


    /**
     * Copies selected rows to the system clipboard.
     */
    public void copySelectedToClipboard() {
        final TransferHandler handler = jTable1.getTransferHandler();
        final Toolkit tk = Toolkit.getDefaultToolkit();
        final Clipboard clipboard = tk.getSystemClipboard();
        handler.exportToClipboard(jTable1, clipboard, TransferHandler.COPY);
    }

    /**
     * 
     * @return the progress bar
     */
    public JProgressBar getProfileProgressBar() {
        return profileProgressBar;
    }

    /**
     * 
     * @return The status progress bar
     */
    public JProgressBar getStatusProgressBar() {
        return statusProgressBar;
    }

    /**
     * 
     * @return the status label
     */
    public JLabel getStatusLabel() {
        return statusLabel;
    }

    /**
     * @return the treeModel
     */
    public DefaultTreeModel getTreeModel() {
        return treeModel;
    }

    /**
     * @param profile
     *            the profile to set
     */
    public void setProfile(ProfileInstance profile) {
        this.profile = profile;
        profile.addEventListener(listener);
        listener.fireEvent(profile);
    }

    /**
     * @return the profile
     */
    public ProfileInstance getProfile() {
        return profile;
    }

    /**
     * Closes a profile.
     */
    public void closeProfile() {
        CloseProfileAction closeAction = new CloseProfileAction(droidMainUi.getProfileManager(), context, this);
        JOptionPaneProxy dialog = () -> {
            int result = JOptionPane.showConfirmDialog(ProfileForm.this, "Save this profile?", "Warning",
                    JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);

            int response = JOptionPaneProxy.CANCEL;
            if (result == JOptionPane.YES_OPTION) {
                response = JOptionPaneProxy.YES;
            } else if (result == JOptionPane.NO_OPTION) {
                response = JOptionPaneProxy.NO;
            }

            return response;
        };

        closeAction.setUserOptionDialog(dialog);
        final JFileChooser fileChooser = context.getProfileFileChooser();
        FileChooserProxy chooserProxy = new FileChooserProxyImpl(this, fileChooser);
        closeAction.setSaveAction(new SaveProfileWorker(droidMainUi.getProfileManager(), this, chooserProxy));
        closeAction.start();
    }

    /**
     * Saves a profile.
     * 
     * @param saveAs
     *            whether to show a file chooser dialog
     */
    public void saveProfile(final boolean saveAs) {
        final JFileChooser fileChooser = context.getProfileFileChooser();
        fileChooser.setDialogTitle(String.format("Save profile '%s'", getName()));
        FileChooserProxy dialog = new FileChooserProxyImpl(this, fileChooser);
        final Path loadedFrom = getProfile().getLoadedFrom();
        fileChooser.setSelectedFile(loadedFrom != null ? loadedFrom.toFile() : new File(getName()));

        final SaveProfileWorker worker = new SaveProfileWorker(droidMainUi.getProfileManager(), this, dialog);
        worker.start(saveAs);
    }

    /**
     * Updates widgets before a save operation.
     */
    public void beforeSave() {
        statusLabel.setText("Saving profile...");
        statusProgressBar.setValue(0);
        statusProgressBar.setIndeterminate(false);
        statusLabel.setVisible(true);
        statusProgressBar.setVisible(true);
    }
    
    
    private void openSelectedFolders() {
        OpenContainingFolderAction openAction = new OpenContainingFolderAction();
        openAction.open(getSelectedNodes());
    }

    /**
     * Updates widgets after a save operation.
     */
    public void afterSave() {
        statusLabel.setVisible(false);
        statusProgressBar.setVisible(false);
    }

    /**
     * Updates state after loading.
     */
    public void afterLoad() {
        throttleSlider.setValue(profile.getThrottle());
        throttlePanel.setVisible(true);
        getStatusProgressBar().setVisible(false);
        getStatusLabel().setVisible(false);
        listener.fireEvent(getProfile());
        droidMainUi.updateFilterControls();
    }

    /**
     * Updates state after creating new profile.
     */
    public void afterCreate() {
        throttleSlider.setValue(profile.getThrottle());
        throttlePanel.setVisible(true);
    }

    /**
     * ] Sets the state change listener.
     * 
     * @param stateChangeListener
     *            the listener to set.
     */
    public void setStateChangeListener(ProfileEventListener stateChangeListener) {
        this.listener = stateChangeListener;
    }

    /**
     * Starts a profile.
     */
    public void start() {
        ProfileManager profileManager = droidMainUi.getProfileManager();
        DefaultMutableTreeNode rootNode = (DefaultMutableTreeNode) treeModel.getRoot();
        inMemoryNodes.put(-1L, rootNode);

        job = new DroidJob();
        job.setProfileManager(profileManager);
        job.setProfileForm(this);
        job.start();
    }

    /**
     * Stops a profile.
     */
    public void stop() {
        ProfileManager profileManager = droidMainUi.getProfileManager();
        profileManager.stop(getProfile().getUuid());
        job.cancel(true);
    }

    /**
     * @return the profileTab
     */
    public ProfileTabComponent getProfileTab() {
        return profileTab;
    }

    /**
     * @return the inMemoryNodes
     */
    public Map<Long, DefaultMutableTreeNode> getInMemoryNodes() {
        return inMemoryNodes;
    }

    /**
     * @return the throttleSlider
     */
    public JSlider getThrottleSlider() {
        return throttleSlider;
    }

    /**
     * @return the throttleLabel
     */
    JLabel getThrottleLabel() {
        return throttleLabel;
    }

    /**
     * @return the listener
     */
    public ProfileEventListener getListener() {
        return listener;
    }

    /**
     * @return the progressPanel
     */
    public JPanel getProgressPanel() {
        return progressPanel;
    }
    
    /**
     * 
     * @return the throttlePanel.
     */
    public JPanel getThrottlePanel() {
        return throttlePanel;
    }
    
    /**
     * @return the droidMainUi
     */
    DroidMainFrame getDroidMainUi() {
        return droidMainUi;
    }

    /**
     * 
     * @return A list of the profile resource nodes selected in the outline.
     */
    public List<ProfileResourceNode> getSelectedNodes() {
        List<ProfileResourceNode> results = new ArrayList<>();
        for (TreeNode node : treeTableModel.getSelectedNodes()) {
            results.add(getProfileResourceNode(node));
        }
        return results;
    }

    /**
     * @return The first selected DefaultMutableTreeNode or null if none selected.
     */
    public DefaultMutableTreeNode getSelectedTreeNode() {
        return anyRowsSelected() ? (DefaultMutableTreeNode) treeTableModel.getSelectedNode() : null;
    }

    /**
     * @return A list of all currently selected tree nodes.
     */
    public List<TreeNode> getSelectedTreeNodes() {
        return anyRowsSelected() ? treeTableModel.getSelectedNodes() : Collections.emptyList();
    }
    
    /**
     * 
     * @return whether any rows are selected in the profile.
     */
    public boolean anyRowsSelected() {
        return jTable1.getSelectedRow() >= 0;
    }

    /**
     * @return A ListSelectionModel from the table.
     */
    public ListSelectionModel getSelectionModel() {
        return jTable1.getSelectionModel();
    }

    /**
     * Expands the selected nodes in the tree.
     * @param recursive whether to expand all children.
     */
    public void expandSelectedNodes(boolean recursive) {
        if (recursive) {
            for (TreeNode node : treeTableModel.getSelectedNodes()) {
                treeTableModel.expandChildren(node, MAX_LEVELS_TO_EXPAND);
            }
        } else {
            for (TreeNode node : treeTableModel.getSelectedNodes()) {
                treeTableModel.expandNode(node);
            }
        }
    }

    /**
     * Refreshes filtering for the profile.
     * Call this once a new filter has been set on the profile to update the display, or a filter has been removed,
     * or is not enabled.
     */
    public void refreshFiltering() {
        if (profileIsFiltered()) {
            updateProfileResourceNodeFilterStatus();
            treeTableModel.setNodeFilter(FILTER_BY_STATUS);
        } else {
            setAllNodesUnfiltered();
            treeTableModel.setNodeFilter(null);
        }
    }

    private void setAllNodesUnfiltered() {
        final TreeNode root = treeTableModel.getRoot();
        TreeUtils.walk(root, node -> getProfileResourceNode(node).setFilterStatus(1), node -> node.getParent() != null);
    }



    private boolean profileIsFiltered() {
        final Filter profileFilter = profile.getFilter();
        return profileFilter != null && profileFilter.isEnabled() && profileFilter.hasCriteria();
    }

    private void updateProfileResourceNodeFilterStatus() {
        // Useful constants:
        final TreeNode rootNode = treeTableModel.getRoot();
        final ProfileManager profileManager = droidMainUi.getProfileManager();

        // Get a collection of parents ids currently represented in the tree nodes:
        final Set<Long> parentIds = new LinkedHashSet<>(); // We use a linked hash set as it will be iterated over later.
        TreeUtils.walk(rootNode, node -> parentIds.add(getProfileResourceNode(node).getId()), TREE_NODE_ALLOWS_CHILDREN);

        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        try {
            // Obtain up-to-date filter status for all of their children from the profile manager:
            // This will return an empty map if filtering is not enabled or has criteria
            final Map<Long, Integer> filterStatus = profileManager.findFilterStatusForChildren(profile.getUuid(), parentIds);

            // If there is nothing being filtered, update all nodes to have a filter status of 1.
            if (filterStatus.isEmpty()) {
                setAllNodesUnfiltered();
            } else { // Update the nodes with the filter status - but not the root node (with a null parent) as it doesn't have a ProfileResourceNode.
                TreeUtils.walk(treeTableModel.getRoot(), node -> updateFilterStatus(node, filterStatus), node -> node.getParent() != null);
            }
        } finally {
            setCursor(Cursor.getDefaultCursor());
        }
    }

    private void updateFilterStatus(final TreeNode treeNode, final Map<Long, Integer> filterStatus) {
        final ProfileResourceNode node = getProfileResourceNode(treeNode);
        final Integer filterValue = filterStatus.get(node.getId());
        if (filterValue != null) {
            node.setFilterStatus(filterValue);
        } else {
            node.setFilterStatus(1); // set to default if not listed.
        }
    }

    /**
     * Generates a URL to an external page from a PUID. 
     * @param puid the puid
     * @return URL to a resource
     */
    String getPronumURLPrefix(String puid) {
        // get it from configuration.
        String puidUrl = droidMainUi.getGlobalContext().getGlobalConfig().getProperties().getString(
                DroidGlobalProperty.PUID_URL_PATTERN.getName());
        return String.format(puidUrl, puid);
    }

    private static ResourceType getResourceType(TreeNode node) {
        final ProfileResourceNode node1 = getProfileResourceNode(node);
        final NodeMetaData metadata = node1.getMetaData();
        return metadata == null ? null : metadata.getResourceType();
    }

    private static ProfileResourceNode getProfileResourceNode(TreeNode node) {
        return (ProfileResourceNode) ((DefaultMutableTreeNode) node).getUserObject();
    }


    private class TreeMouseAdapter extends MouseAdapter {
        @Override
        public void mouseReleased(MouseEvent e) {

            if (e.getButton() == MouseEvent.BUTTON3) {
                final Point mousePoint = e.getPoint();
                final int rowIndex = jTable1.rowAtPoint(mousePoint);
                if (rowIndex > -1 && !jTable1.isRowSelected(rowIndex)) {
                    jTable1.setRowSelectionInterval(rowIndex, rowIndex);
                }
                jPopupMenu1.show(jTable1, e.getX(), e.getY());
            } else {
                final Point mousePoint = e.getPoint();
                final int colIndex = jTable1.columnAtPoint(mousePoint);
                if (colIndex == jTable1.getColumn(PUID_COLUMN_NAME).getModelIndex()) {
                    final int rowIndex = jTable1.rowAtPoint(mousePoint);
                    final Object cellObj = jTable1.getValueAt(rowIndex, colIndex);
                    if (cellObj != null) {
                        String cellValue = cellObj.toString();
                        cellValue = cellValue.replace(PUID_VALUE_PREFIX, "");
                        cellValue = cellValue.replace(PUID_VALUE_SUFFIX, "");
                        cellValue.trim();
                        if (cellValue.startsWith("\"")) {
                            String[] puids = StringUtils.split(cellValue, ",");
                            for (String puid : puids) {
                                String unquotedPuid = StringUtils.strip(puid, "\" ");
                                openURL(getPronumURLPrefix(unquotedPuid));
                            }
                        } else {
                            if (cellValue.length() > 0) {
                                openURL(getPronumURLPrefix(cellValue));
                            }
                        }
                    }
                }
            }
        }

        public void openURL(String puidUrl) {
            Desktop desktop = null;
            if (Desktop.isDesktopSupported()) {
                desktop = Desktop.getDesktop();
                if (desktop.isSupported(Desktop.Action.BROWSE)) {
                    try {
                        URL url = new URL(puidUrl);
                        desktop.browse(url.toURI());
                    } catch (MalformedURLException e1) {
                        DialogUtils
                                .showGeneralErrorDialog(droidMainUi, "MalformedURLException", "Invalid URL.");
                    } catch (IOException e1) {
                        DialogUtils.showGeneralErrorDialog(droidMainUi, "IOException", "Resource not found.");
                    } catch (URISyntaxException uriSyntaxEx) {
                        DialogUtils.showGeneralErrorDialog(droidMainUi, "URISyntaxException", "Invalid URI.");
                    }
                }
            }
        }

        @Override
        public void mouseMoved(MouseEvent e) {

            final Point mousePoint = e.getPoint();
            final int colIndex = jTable1.columnAtPoint(mousePoint);
            final int rowIndex = jTable1.rowAtPoint(mousePoint);
            final int colModelIndex = jTable1.convertColumnIndexToModel(
                    jTable1.columnAtPoint(mousePoint));
            final Object cellObject = jTable1.getValueAt(rowIndex, colIndex);
            if (cellObject != null) {
                String cellValue = cellObject.toString();
                jTable1.setToolTipText(cellValue);
                if (colModelIndex == jTable1.getColumn(PUID_COLUMN_NAME).getModelIndex()) {
                    cellValue = jTable1.getValueAt(rowIndex, colIndex).toString();
                    cellValue = cellValue.replace(PUID_VALUE_PREFIX, "");
                    cellValue = cellValue.replace(PUID_VALUE_SUFFIX, "");
                    cellValue.trim();
                    if (cellValue.length() > 0) {
                        setCursor(new Cursor(Cursor.HAND_CURSOR));
                    } else {
                        setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                    }
                } else if (colModelIndex == jTable1.getColumn(IDENTIFICATIONS_COLUMN_NAME).getModelIndex()) {
                    Integer value = (Integer) jTable1.getValueAt(rowIndex, colIndex);
                    if (value != null && value > 1) {
                        setCursor(new Cursor(Cursor.HAND_CURSOR));
                    } else {
                        setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                    }
                } else {
                    setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                }
                
                repaint();
            }
        }

        public void mouseClicked(MouseEvent e) {
            Point mousePoint = e.getPoint();
            int colIndex = jTable1.columnAtPoint(mousePoint);
            int rowIndex = jTable1.rowAtPoint(mousePoint);
            int colModelIndex = jTable1.convertColumnIndexToModel(jTable1.columnAtPoint(mousePoint));

            if (colModelIndex == jTable1.getColumn(IDENTIFICATIONS_COLUMN_NAME).getModelIndex()) {
                Integer count = (Integer) jTable1.getValueAt(rowIndex, colIndex);
                if (count != null && count > 1) {
                    DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) treeTableModel.getNodeAtTableRow(rowIndex);
                    ProfileResourceNode node = (ProfileResourceNode) treeNode.getUserObject();
                    multiIdentificationDialog.showDialog(node);
                }
            }
        }
    }

}
