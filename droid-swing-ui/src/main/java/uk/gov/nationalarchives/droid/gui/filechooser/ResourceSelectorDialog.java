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
package uk.gov.nationalarchives.droid.gui.filechooser;

import java.awt.Component;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import java.io.File;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;

import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.SwingConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.filechooser.FileSystemView;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import uk.gov.nationalarchives.droid.util.FileUtil;

/**
 *
 * @author rflitcroft
 */
public class ResourceSelectorDialog extends JDialog {
    
    private static final int FILE_COLUMN_INDEX = 0;
    private static final int SIZE_COLUMN_INDEX = 1;
    private static final int DATE_COLUMN_INDEX = 2;

    private static final int WIDE_COLUMN = 200;
    private static final int NARROW_COLUMN = 100;

    private static final long serialVersionUID = -8906890136444606083L;

    private static final char QUOTE = '"';
    
    private static final Object[] COLUMN_NAMES = new Object[] {
        "Name", "Size", "Last modified", };
        
    private static final Class<?>[] TYPES = new Class [] {
        File.class, Long.class, Date.class, };    
    
    private FileSystemView fsv = FileSystemView.getFileSystemView();
    
    private List<File> selectedFiles = new ArrayList<File>();
    
    private int response = JFileChooser.CANCEL_OPTION;
    
    /** 
     * Creates new form ResourceSelector.
     * @param parent parent window
     */
    public ResourceSelectorDialog(Window parent) {
        super(parent);
        initComponents();
        initTree();
        initTable();
        pack();
    }
    
    /**
     * Shows the file chooser Dialog.
     * @param parent the dialog's parent
     * @return the response.
     */
    public int showDialog(Component parent) {
        setLocationRelativeTo(parent);
        //table.clearSelection();
        //selectedFilesTextBox.setText(null);
        //selectedFiles.clear();
        response = JFileChooser.CANCEL_OPTION;
        setVisible(true);
        return response;
    }
    
    /**
     * @return the response
     */
    public int getResponse() {
        return response;
    }
    
    /**
     * 
     * @return true if select subfolders was checked; false otherwise
     */
    public boolean isSelectionRecursive() {
        return subfoldersCheckBox.isSelected();
    }

    /**
     * @return the selectedFiles
     */
    public List<File> getSelectedFiles() {
        return selectedFiles;
    }
    
    private DefaultTreeModel getTreeModel() {
        DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode(new File("File System"), true);
        final DefaultTreeModel treeModel = new DefaultTreeModel(rootNode, true);
        return treeModel;
        
    }
    
    private void initTree() {
        tree.setCellRenderer(new FileTreeCellRenderer());
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        DefaultTreeModel treeModel = (DefaultTreeModel) tree.getModel();
        DefaultMutableTreeNode rootNode = (DefaultMutableTreeNode) treeModel.getRoot();
        
        File[] roots = fsv.getRoots();
        //File[] roots = File.listRoots();
        for (int i = 0; i < roots.length; i++) {
            File rootFile = roots[i];
            boolean rootAllowsChildren = rootFile.isDirectory();
            final DefaultMutableTreeNode newChild = new DefaultMutableTreeNode(rootFile, rootAllowsChildren);
            if (rootAllowsChildren) {
                rootNode.add(newChild);
            }
            
            File[] children = rootFile.listFiles();
            if (children != null) {
                List<File> sortedChildren = ResourceDialogUtil.sortFiles(children);
                
                for (File child : sortedChildren) {
                    final File[] listFiles = child.listFiles();
                    if (child.isDirectory()) {
                        boolean allowsChildren = listFiles != null && listFiles.length > 0;
                        DefaultMutableTreeNode subFolder = new DefaultMutableTreeNode(child, allowsChildren);
                        newChild.add(subFolder);
                    }
                }
            }
            tree.expandPath(new TreePath(newChild.getPath()));
        }
        
        treeModel.nodeStructureChanged(rootNode);
        
        
        tree.addTreeWillExpandListener(new ResourceTreeWillExpandListener());
        tree.addTreeSelectionListener(new FileTreeSelectionListener());
        
    }
    
    @SuppressWarnings("serial")
    private void initTable() {
        table.setDefaultRenderer(File.class, new DefaultTableCellRenderer() {
            @Override
            protected void setValue(Object value) {
                File f = (File) value;
                setIcon(fsv.getSystemIcon(f));
                setText(fsv.getSystemDisplayName(f));
            }
        });
        
        table.setDefaultRenderer(Long.class, new DefaultTableCellRenderer() {
            @Override
            protected void setValue(Object value) {
                setHorizontalAlignment(SwingConstants.RIGHT);
                setText(value == null ? "" : FileUtil.formatFileSize((Long) value, 1));
            }
        });

        table.setDefaultRenderer(Date.class, new DefaultTableCellRenderer() {
            @Override
            protected void setValue(Object value) {
                setText(value == null ? "" : DateFormat.getDateTimeInstance().format(value));
            }
        });

        table.addMouseListener(new DoubleClickMouseAdapter());
        
        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    int[] selectedRows = table.getSelectedRows();
                    
                    selectedFiles.clear();
                    for (int i : selectedRows) {
                        selectedFiles.add((File) table.getValueAt(i, 0));
                    }
                    selectedFilesTextBox.setText(toText(selectedFiles));
                }
            }
        });
        
        table.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
        
        table.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER && table.getSelectedColumnCount() == 1) {
                    exploreTableFolder(table.getSelectedRow());
                }
            }
        });
        
    }
    
    private String toText(List<File> files) {
        StringBuilder sb = new StringBuilder();
        for (File f : files) {
            //sb.append(QUOTE + f.getName() + QUOTE + ' ');
            sb.append(QUOTE + fsv.getSystemDisplayName(f) + QUOTE + ' ');
        }
        return sb.toString();
    }
    
    private class DoubleClickMouseAdapter extends MouseAdapter {
        @Override
        public void mouseClicked(MouseEvent event) {
            if (event.getClickCount() == 2) {
                int clickedRow = table.rowAtPoint(event.getPoint());
                if (clickedRow != -1) {
                    exploreTableFolder(clickedRow);
                }
            }
        }
    }
    
    private void exploreTableFolder(int rowIndex) {
        File f = (File) table.getValueAt(rowIndex, 0);
        if (f.isDirectory()) {
            final TreePath selectionPath = tree.getSelectionPath();
            DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode)
                selectionPath.getLastPathComponent();
            for (Enumeration<TreeNode> e = treeNode.children(); e.hasMoreElements();) {
                DefaultMutableTreeNode n = (DefaultMutableTreeNode) e.nextElement();
                if (n.getUserObject().equals(f)) {
                    final TreePath path = new TreePath(n.getPath());
                    tree.expandPath(path);
                    tree.setSelectionPath(path);
                    tree.scrollPathToVisible(path);
                    break;
                }
            }
        }
    }
    
    private TableModel updateTable(File[] contents) {


        DefaultTableModel tableModel = new DefaultTableModel(COLUMN_NAMES, 0) {
            
            private static final long serialVersionUID = -1940634018074783780L;

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return TYPES[columnIndex];
            }
            
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        for (File f : contents) {
            tableModel.addRow(new Object[] {
                f, 
                f.isDirectory() ? null : f.length(), 
                f.lastModified() == 0 ? null : new Date(f.lastModified()),
            });
        }
        

        
        return tableModel;
    }
    
    private void initColumnModel() {
        final TableColumnModel columnModel = table.getColumnModel();
        final TableColumn fileColumn = columnModel.getColumn(FILE_COLUMN_INDEX);
        
        fileColumn.setPreferredWidth(WIDE_COLUMN);
        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("uk/gov/nationalarchives/droid/gui/filechooser/Bundle");
        fileColumn.setHeaderValue(bundle.getString("ResourceSelector.table.columnModel.title0"));
               
        columnModel.getColumn(SIZE_COLUMN_INDEX).setPreferredWidth(NARROW_COLUMN);
        columnModel.getColumn(SIZE_COLUMN_INDEX).setHeaderValue(bundle.getString("ResourceSelector.table.columnModel.title1"));
        
        columnModel.getColumn(DATE_COLUMN_INDEX).setPreferredWidth(WIDE_COLUMN);
        columnModel.getColumn(DATE_COLUMN_INDEX).setHeaderValue(bundle.getString("ResourceSelector.table.columnModel.title3"));
    }
    
    private class FileTreeCellRenderer extends DefaultTreeCellRenderer {
        private static final long serialVersionUID = -971847725978939749L;

        @Override
        public Component getTreeCellRendererComponent(JTree parentTree, Object value, 
                boolean sel, boolean expanded,
                boolean leaf, int row, boolean hasFocus) {
            File f = (File) ((DefaultMutableTreeNode) value).getUserObject();
            if (f.exists()) {
                setIcon(fsv.getSystemIcon(f));
                final String systemDisplayName = fsv.getSystemDisplayName(f);
                setText(systemDisplayName.isEmpty() ? f.getPath() : systemDisplayName);
                if (sel) {
                    setForeground(getTextSelectionColor());
                } else {
                    setForeground(getTextNonSelectionColor());
                }
                selected = sel;
                return this;
            }
            return super.getTreeCellRendererComponent(parentTree, value, sel, expanded, leaf, row, hasFocus);
        }
    }
    
    private class FileTreeSelectionListener implements TreeSelectionListener {
        @Override
        public void valueChanged(TreeSelectionEvent e) {
            TreePath treePath = e.getNewLeadSelectionPath();
            if (treePath != null) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) treePath.getLastPathComponent();
                File f = (File) node.getUserObject();
                final File[] listFiles = f.listFiles();
                if (listFiles != null) {
                    table.setModel(updateTable(listFiles));
                    TableRowSorter<TableModel> sorter = new TableRowSorter<TableModel>(table.getModel());
                    sorter.setComparator(0, new Comparator<File>() {
                        @Override
                        public int compare(File f1, File f2) {
                            Boolean f1Dir = f1.isDirectory();
                            Boolean f2Dir = f2.isDirectory();

                            int comp = f2Dir.compareTo(f1Dir);
                            if (comp == 0) {
                                comp = f1.getName().compareToIgnoreCase(f2.getName());
                            }
                            
                            return comp;
                        }        
                    });
                    table.setRowSorter(sorter);
                    sorter.sort();
                    sorter.toggleSortOrder(0);

                    initColumnModel();
                    tree.expandPath(treePath);
                    
                } else {
                    table.setModel(new DefaultTableModel());
                }
                
                // Selection in the navigator tree selects a folder for profiling:
                selectedFiles.clear();
                selectedFiles.add(f);
                //selectedFilesTextBox.setText(QUOTE + f.getPath() + QUOTE);
                selectedFilesTextBox.setText(QUOTE + fsv.getSystemDisplayName(f) + QUOTE);
                
            }
        }
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jSplitPane1 = new javax.swing.JSplitPane();
        jScrollPane2 = new javax.swing.JScrollPane();
        table = new javax.swing.JTable();
        jPanel2 = new javax.swing.JPanel();
        jPanel3 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        tree = new javax.swing.JTree();
        jPanel4 = new javax.swing.JPanel();
        selectedFilesTextBox = new javax.swing.JTextField();
        subfoldersCheckBox = new javax.swing.JCheckBox();
        jLabel2 = new javax.swing.JLabel();
        okButton = new javax.swing.JButton();
        cancelButton = new javax.swing.JButton();

        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("uk/gov/nationalarchives/droid/gui/filechooser/Bundle"); // NOI18N
        setTitle(bundle.getString("ResourceSelectorDialog.title")); // NOI18N

        jSplitPane1.setDividerLocation(200);

        table.setModel(new DefaultTableModel());
        table.setFillsViewportHeight(true);
        table.setIntercellSpacing(new java.awt.Dimension(4, 1));
        table.setShowHorizontalLines(false);
        table.setShowVerticalLines(false);
        jScrollPane2.setViewportView(table);

        jSplitPane1.setRightComponent(jScrollPane2);

        jPanel3.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.RAISED));

        jLabel1.setText(bundle.getString("ResourceSelector.jLabel1.text")); // NOI18N

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jLabel1, javax.swing.GroupLayout.DEFAULT_SIZE, 193, Short.MAX_VALUE)
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 4, Short.MAX_VALUE)
                .addContainerGap())
        );

        tree.setModel(getTreeModel());
        tree.setLargeModel(true);
        tree.setRootVisible(false);
        jScrollPane1.setViewportView(tree);

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 199, Short.MAX_VALUE)
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1))
        );

        jSplitPane1.setLeftComponent(jPanel2);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jSplitPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 717, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jSplitPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 352, Short.MAX_VALUE)
                .addContainerGap())
        );

        selectedFilesTextBox.setEditable(false);
        selectedFilesTextBox.setText(bundle.getString("ResourceSelector.jTextField1.text")); // NOI18N

        subfoldersCheckBox.setSelected(true);
        subfoldersCheckBox.setText(bundle.getString("ResourceSelector.jCheckBox1.text")); // NOI18N

        jLabel2.setText(bundle.getString("ResourceSelector.jLabel2.text")); // NOI18N

        okButton.setText(bundle.getString("ResourceSelector.okButton.text")); // NOI18N
        okButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                okButtonActionPerformed(evt);
            }
        });

        cancelButton.setText(bundle.getString("ResourceSelector.cancelButton.text")); // NOI18N
        cancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addComponent(jLabel2)
                .addGap(18, 18, 18)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(subfoldersCheckBox, javax.swing.GroupLayout.DEFAULT_SIZE, 522, Short.MAX_VALUE)
                    .addComponent(selectedFilesTextBox, javax.swing.GroupLayout.DEFAULT_SIZE, 522, Short.MAX_VALUE))
                .addGap(18, 18, 18)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(cancelButton)
                    .addComponent(okButton, javax.swing.GroupLayout.PREFERRED_SIZE, 68, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        jPanel4Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {cancelButton, okButton});

        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addGap(17, 17, 17)
                        .addComponent(jLabel2))
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(okButton)
                            .addComponent(selectedFilesTextBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(cancelButton)
                    .addComponent(subfoldersCheckBox))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel4Layout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {cancelButton, okButton});

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(layout.createSequentialGroup()
                .addGap(10, 10, 10)
                .addComponent(jPanel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void okButtonActionPerformed(ActionEvent evt) {//GEN-FIRST:event_okButtonActionPerformed
        response = JFileChooser.APPROVE_OPTION;
        setVisible(false);
    }//GEN-LAST:event_okButtonActionPerformed

    private void cancelButtonActionPerformed(ActionEvent evt) {//GEN-FIRST:event_cancelButtonActionPerformed
        setVisible(false);
    }//GEN-LAST:event_cancelButtonActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton cancelButton;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JSplitPane jSplitPane1;
    private javax.swing.JButton okButton;
    private javax.swing.JTextField selectedFilesTextBox;
    private javax.swing.JCheckBox subfoldersCheckBox;
    private javax.swing.JTable table;
    private javax.swing.JTree tree;
    // End of variables declaration//GEN-END:variables

}
