/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.equinix.amphibia.components;

import static com.equinix.amphibia.components.TreeCollection.TYPE.*;
import static com.equinix.amphibia.components.JTreeTable.EditValueRenderer.TYPE.*;

import com.equinix.amphibia.IO;
import com.equinix.amphibia.Amphibia;
import com.sksamuel.diffpatch.DiffMatchPatch;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.JToggleButton;
import javax.swing.JTree;
import javax.swing.ListSelectionModel;
import javax.swing.OverlayLayout;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.text.BadLocationException;
import javax.swing.text.StyledDocument;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSON;
import org.apache.commons.io.IOUtils;

/**
 *
 * @author dgofman
 */
public final class Editor extends BaseTaskPane {

    private static final DiffMatchPatch DIFF = new DiffMatchPatch();
    private static final byte LINE_SEPARATOR = '\u0001';

    private final List<String[]> histories = new ArrayList<>();
    private int historyIndex;

    private  JTreeTable treeTable;
    private  JSONTableModel defaultModel;
    private  DefaultTableModel historyModel;
    private DefaultComboBoxModel serversModel;

    private RandomAccessFile historyWriter;
    private JSONTableModel jsonModel;
    
    public int loadMaxLastHistory;

    /**
     * Creates new form Converter
     */
    @SuppressWarnings("OverridableMethodCallInConstructor")
    public Editor() {
        super();
        
        serversModel = new DefaultComboBoxModel<>(new String[] { bundle.getString("mockServer") });

        historyModel = new DefaultTableModel(
                new Object[][]{},
                new String[]{
                    bundle.getString("date"), bundle.getString("file"), bundle.getString("content"), ""
                }
        );

        defaultDividerLocation = 300;
        loadMaxLastHistory = userPreferences.getInt(Amphibia.P_HISTORY, 50);
        
        try {
            File history = new File(".history");
            historyIndex = 0;
            if (history.exists()) {
                int counter = 0;
                historyWriter = new RandomAccessFile(history, "rw");
                long length = historyWriter.length() - 1;
                while (length > 0 && counter++ < loadMaxLastHistory) {
                    byte b = 0;
                    StringBuilder sb = new StringBuilder();
                    while (b != LINE_SEPARATOR && length > 0) {
                        length -= 1;
                        historyWriter.seek(length);
                        b = historyWriter.readByte();
                        if (b != LINE_SEPARATOR) {
                            sb.append((char) b);
                        }
                    }
                    String line = "";
                    try {
                        line = sb.reverse().toString();
                        int idx1 = line.indexOf("\t");
                        String time = line.substring(0, idx1);
                        Date date = new Date(Long.valueOf(time));
                        int idx2 = line.indexOf("\t", idx1 + 1);
                        String file = line.substring(idx1 + 1, idx2);
                        historyModel.addRow(new Object[]{dateFormat.print(date.getTime()), file, line.substring(idx2 + 1)});
                    } catch (NullPointerException | NumberFormatException | StringIndexOutOfBoundsException e) {
                        logger.log(Level.SEVERE, "Line number: " + counter + "::" + line, e);
                    }
                }
                historyWriter.close();
            }

            historyWriter = new RandomAccessFile(history, "rw");
            historyWriter.seek(history.length());
        } catch (IOException ex) {
            logger.log(Level.SEVERE, null, ex);
        }

        initComponents();

        Amphibia.setDefaultHTMLStyles(txtRaw);

        defaultModel = JSONTableModel.createModel(bundle);
        JTreeTable.RowEventListener rowListener = (JTreeTable table, int row, int column, Object cellValue) -> {
            TreeIconNode node = MainPanel.selectedNode;
            if (node.getParent() == null) {
                return;
            }
            TreeCollection.TYPE type = node.getType();
            TreeCollection collection = node.getCollection();
            Entry entry = (Entry) table.getModel().getValueAt(row, 0);
            Object value = table.getModel().getValueAt(row, 1);
            if (cellValue == ADD) {
                mainPanel.resourceEditDialog.openCreateDialog(entry);
            } else if (cellValue == VIEW) {
                mainPanel.resourceEditDialog.openEditDialog(entry, value, false);
            } else if (cellValue == EDIT || cellValue == EDIT_LIMIT) {
                if (entry.parent != null && entry.parent.type == TRANSFER) {
                    mainPanel.transferDialog.openDialog(MainPanel.selectedNode, entry);
                } else {
                    mainPanel.resourceEditDialog.openEditDialog(entry, value, true);
                }
            } else if (cellValue == ADD_RESOURCES) {
                if (type == TESTSUITE) {
                    mainPanel.resourceAddDialog.showTestCaseDialog(node);
                } else {
                    mainPanel.resourceAddDialog.showTestStepDialog(node);
                }
            } else if (cellValue == REFERENCE_EDIT) {
                if ((type == TESTSUITE && "testcases".equals(entry.parent.name)) || 
                    (type == TESTCASE && "teststeps".equals(entry.parent.name))) {
                    mainPanel.resourceOrderDialog.openDialog(node, entry.getParent().getIndex(entry));
                } else {
                   mainPanel.referenceEditDialog.openEditDialog(collection, entry);
                }
            } else if (cellValue == REFERENCE) {
                mainPanel.referenceEditDialog.openViewDialog(collection, entry);
            } else if (cellValue == TRANSFER) {
                mainPanel.transferDialog.openDialog(MainPanel.selectedNode, entry);
            }
        };
        treeTable = new JTreeTable(defaultModel, rowListener);
        treeTable.setAutoCreateColumnsFromModel(false);
        treeTable.getColumnModel().getColumn(0).setPreferredWidth(300);
        treeTable.setAutoResizeMode(JTable.AUTO_RESIZE_NEXT_COLUMN);
        treeTable.getColumnModel().getColumn(1).setPreferredWidth(500);
        treeTable.getColumnModel().getColumn(2).setResizable(false);
        treeTable.getColumnModel().getColumn(2).setMaxWidth(20);
        treeTable.setShowVerticalLines(true);
        treeTable.setFillsViewportHeight(true);
        treeTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = treeTable.rowAtPoint(e.getPoint());
                    int col = treeTable.columnAtPoint(e.getPoint());
                    Object cellValue = treeTable.getValueAt(row, 2);
                    rowListener.fireEvent(treeTable, row, col, cellValue);
                }
            }
        });
        pnlTop.add(new JScrollPane(treeTable));

        JTree tableTree = treeTable.getTree();
        tableTree.setShowsRootHandles(true);
        tableTree.setRootVisible(false);

        int buttonIndex = 3;
        JButton button = new JButton("...");
        Font font = treeTable.getFont();
        TableCellRenderer renderer = tblHistory.getDefaultRenderer(Object.class);
        Cursor handCursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
        Cursor defaultCursor = Cursor.getDefaultCursor();

        treeTable.getTableHeader().setFont(font.deriveFont(Font.BOLD));
        treeTable.getTableHeader().setReorderingAllowed(false);
        
        tblHistory.getTableHeader().setFont(font.deriveFont(Font.BOLD));
        tblHistory.setAutoCreateColumnsFromModel(false);
        tblHistory.getTableHeader().setResizingAllowed(true);
        tblHistory.getTableHeader().setReorderingAllowed(false);
        tblHistory.getColumnModel().getColumn(0).setPreferredWidth(130);
        tblHistory.getColumnModel().getColumn(1).setPreferredWidth(255);
        tblHistory.setAutoResizeMode(JTable.AUTO_RESIZE_NEXT_COLUMN);
        tblHistory.getColumnModel().getColumn(2).setPreferredWidth(600);
        tblHistory.getColumnModel().getColumn(buttonIndex).setResizable(false);
        tblHistory.getColumnModel().getColumn(buttonIndex).setMaxWidth(20);
        tblHistory.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        tblHistory.setDefaultRenderer(Object.class, (JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) -> {
            if (column == buttonIndex) {
                return button;
            } else {
                Component c = renderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                ((JComponent) c).setToolTipText(value.toString());
                return c;
            }
        });
        tblHistory.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int column = tblHistory.columnAtPoint(e.getPoint());
                if (column == buttonIndex) {
                    tblHistory.setCursor(handCursor);
                } else {
                    tblHistory.setCursor(defaultCursor);
                }
            }
        });

        tblHistory.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int column = tblHistory.columnAtPoint(e.getPoint());
                if (column == buttonIndex) {
                    int row = tblHistory.rowAtPoint(e.getPoint());
                    String filePath = tblHistory.getValueAt(row, 1).toString();
                    String oldContent = tblHistory.getValueAt(row, 2).toString();
                    try {
                        File file = new File(filePath);
                        File backup = IO.getBackupFile(file);
                        if (backup.exists()) {
                            file = backup;
                        }
                        mainPanel.resourceEditDialog.openEditDialog(null, filePath, DIFF.patch_toText(DIFF.patch_make(IO.readFile(file), oldContent)), false);
                    } catch (IOException ex) {
                        addError(ex);
                    }
                }
            }
        });

        setComponents(tbpOutput, treeProblems);
    }
    
    public boolean addHistory(Date date, String filePath, String oldContent, String newContent) {
        if (date == null) {
            date = new Date();
        }
        if (newContent == null || newContent.equals(oldContent)) {
            return false;
        }
        Object[] items = new Object[]{dateFormat.print(date.getTime()), filePath, oldContent, null};
        while (historyIndex > 0) {
            historyModel.removeRow(0);
            histories.remove(0);
            try {
                long length = historyWriter.length() - 1;
                byte b = 0;
                while (b != 10 && length > 0) {
                    length -= 1;
                    historyWriter.seek(length);
                    b = historyWriter.readByte();
                }
                historyWriter.setLength(length == 0 ? 0 : length + 1);
            } catch (IOException ex) {
                addError(ex);
            }
            historyIndex--;
        }
        Amphibia.instance.enableUndo(true);
        Amphibia.instance.enableRedo(false);
        if (histories.size() > 0) {
            histories.remove(0); //remove last saved content
        }
        histories.add(0, new String[]{oldContent, filePath});
        histories.add(0, new String[]{newContent, filePath});
        historyModel.insertRow(0, items);
        if (historyWriter != null) {
            try {
                historyWriter.writeBytes(date.getTime() + "\t" + filePath + "\t" + oldContent);
                historyWriter.writeByte(LINE_SEPARATOR);
            } catch (IOException ex) {
                addError(ex);
            }
        }
        return true;
    }
    
    @Override
    public DefaultMutableTreeNode addError(String error) {
        return super.addError(error);
    }

    public boolean undo() {
        tblHistory.setRowSelectionInterval(0, historyIndex);
        return patchHistory(histories.get(++historyIndex), historyIndex < histories.size() - 1);
    }

    public boolean redo() {
        if (--historyIndex > 0) {
            tblHistory.setRowSelectionInterval(0, historyIndex - 1);
        } else {
            tblHistory.clearSelection();
        }
        return patchHistory(histories.get(historyIndex), historyIndex > 0);
    }

    public boolean patchHistory(String[] item, boolean isEnabled) {
        File file = new File(item[1]);
        try {
            IOUtils.write(item[0], new FileOutputStream(file));
            mainPanel.reloadAll();
            return isEnabled;
        } catch (IOException ex) {
            addError(ex);
        }
        return false;
    }

    public void reloadTable() {
        treeTable.setModel(JSONTableModel.cloneModel(jsonModel));
    }

    public void selectedTreeNode(TreeIconNode node) {
        TreeIconNode.TreeIconUserObject userObject = node.getTreeIconUserObject();
        txtInfo.setText(userObject.getFullPath());
        if (userObject.properties != null) {
            jsonModel = JSONTableModel.createModel(bundle).updateModel(userObject.json, userObject.properties);
            treeTable.setModel(jsonModel);
        }
    }
    
    @Override
    public void clear() {
        if (tabs.getSelectedIndex() == Amphibia.TAB_CONSOLE) {
            mainPanel.profile.resetConsole();
        } else {
            super.clear();
        }
    }

    @Override
    public void refresh() {
        tree.updateUI();
        treeProblems.updateUI();
        treeTable.updateUI();
    }

    @Override
    public void reset() {
        super.reset();
        txtInfo.setText("");
        txtRaw.setText("");
        treeTable.setModel(defaultModel);
    }

    public void resetHistory() {
        histories.clear();
        historyIndex = 0;
        Amphibia.instance.enableUndo(false);
        Amphibia.instance.enableRedo(false);
    }
    
    public void deleteHistory() {
        try {
            historyWriter.setLength(0);
        } catch (IOException ex) {
            addError(ex);
        }
        historyModel.getDataVector().clear();
        historyModel.fireTableStructureChanged();
        resetHistory();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        pnlTop = new JPanel();
        pnlInfo = new JPanel();
        txtInfo = new JTextField();
        pnlEditor = new JPanel();
        pnlOutput = new JPanel();
        tbpOutput = new JTabbedPane();
        srlProblems = new JScrollPane();
        treeProblems = new JTree();
        spnRaw = new JScrollPane();
        txtRaw = new JEditorPane();
        spnConsole = new JScrollPane();
        txtConsole = new JTextPane();
        pnlServers = new JPanel();
        pnlServersTop = new JPanel();
        cmbServers = new JComboBox<>();
        btnStart = new JToggleButton();
        btnStop = new JButton();
        spnServers = new JScrollPane();
        txtServers = new JTextPane();
        pnlProfile = new JPanel();
        jScrollPane1 = new JScrollPane();
        jTextArea1 = new JTextArea();
        splHistory = new JScrollPane();
        tblHistory = new JTable();
        pnlTabRightButtons = new JPanel();
        lblClear = new JLabel();

        setDividerLocation(300);
        setDividerSize(3);
        setOrientation(JSplitPane.VERTICAL_SPLIT);

        pnlTop.setLayout(new BorderLayout());

        pnlInfo.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        pnlInfo.setLayout(new BorderLayout());

        txtInfo.setEditable(false);
        txtInfo.setBackground(new Color(255, 255, 255));
        pnlInfo.add(txtInfo, BorderLayout.CENTER);

        pnlTop.add(pnlInfo, BorderLayout.PAGE_START);
        pnlTop.add(pnlEditor, BorderLayout.CENTER);

        setLeftComponent(pnlTop);

        pnlOutput.setLayout(new OverlayLayout(pnlOutput));

        tbpOutput.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent evt) {
                tbpOutputStateChanged(evt);
            }
        });
        tbpOutput.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent evt) {
                tbpOutputMouseClicked(evt);
            }
        });

        treeProblems.setModel(treeProblemsModel);
        treeProblems.setRootVisible(false);
        srlProblems.setViewportView(treeProblems);

        ResourceBundle bundle = ResourceBundle.getBundle("com/equinix/amphibia/messages"); // NOI18N
        tbpOutput.addTab(bundle.getString("problems"), new ImageIcon(getClass().getResource("/com/equinix/amphibia/icons/error_16.png")), srlProblems); // NOI18N

        txtRaw.setEditable(false);
        txtRaw.setContentType("text/html"); // NOI18N
        spnRaw.setViewportView(txtRaw);

        tbpOutput.addTab(bundle.getString("raw"), new ImageIcon(getClass().getResource("/com/equinix/amphibia/icons/raw_16.png")), spnRaw); // NOI18N

        spnConsole.setViewportView(txtConsole);

        tbpOutput.addTab(bundle.getString("console"), new ImageIcon(getClass().getResource("/com/equinix/amphibia/icons/console_16.png")), spnConsole); // NOI18N

        pnlServers.setLayout(new BorderLayout());

        pnlServersTop.setLayout(new FlowLayout(FlowLayout.LEFT));

        cmbServers.setFont(new Font("Tahoma", 1, 11)); // NOI18N
        cmbServers.setModel(serversModel);
        cmbServers.setPreferredSize(new Dimension(200, 24));
        pnlServersTop.add(cmbServers);

        btnStart.setIcon(new ImageIcon(getClass().getResource("/com/equinix/amphibia/icons/run_16.png"))); // NOI18N
        btnStart.setToolTipText(bundle.getString("start")); // NOI18N
        btnStart.setFocusable(false);
        btnStart.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                btnStartActionPerformed(evt);
            }
        });
        pnlServersTop.add(btnStart);

        btnStop.setIcon(new ImageIcon(getClass().getResource("/com/equinix/amphibia/icons/stop-16.png"))); // NOI18N
        btnStop.setToolTipText(bundle.getString("stop")); // NOI18N
        btnStop.setFocusable(false);
        btnStop.setHorizontalTextPosition(SwingConstants.CENTER);
        btnStop.setVerticalTextPosition(SwingConstants.BOTTOM);
        btnStop.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                btnStopActionPerformed(evt);
            }
        });
        pnlServersTop.add(btnStop);

        pnlServers.add(pnlServersTop, BorderLayout.NORTH);

        spnServers.setViewportView(txtServers);

        pnlServers.add(spnServers, BorderLayout.CENTER);

        tbpOutput.addTab(bundle.getString("servers"), new ImageIcon(getClass().getResource("/com/equinix/amphibia/icons/servers_16.png")), pnlServers); // NOI18N

        pnlProfile.setLayout(new BorderLayout());

        jTextArea1.setColumns(20);
        jTextArea1.setRows(5);
        jScrollPane1.setViewportView(jTextArea1);

        pnlProfile.add(jScrollPane1, BorderLayout.CENTER);

        tbpOutput.addTab(bundle.getString("profile"), new ImageIcon(getClass().getResource("/com/equinix/amphibia/icons/stopwatch_16.png")), pnlProfile); // NOI18N

        tblHistory.setModel(historyModel        );
        splHistory.setViewportView(tblHistory);

        tbpOutput.addTab(bundle.getString("history"), new ImageIcon(getClass().getResource("/com/equinix/amphibia/icons/history_16.png")), splHistory); // NOI18N

        pnlOutput.add(tbpOutput);

        pnlTabRightButtons.setLayout(new FlowLayout(FlowLayout.RIGHT, 10, 3));

        lblClear.setIcon(new ImageIcon(getClass().getResource("/com/equinix/amphibia/icons/clear.png"))); // NOI18N
        lblClear.setToolTipText(bundle.getString("clear")); // NOI18N
        lblClear.setCursor(new Cursor(Cursor.HAND_CURSOR));
        lblClear.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent evt) {
                lblClearMouseClicked(evt);
            }
        });
        pnlTabRightButtons.add(lblClear);

        pnlOutput.add(pnlTabRightButtons);

        setRightComponent(pnlOutput);
    }// </editor-fold>//GEN-END:initComponents

    private void lblClearMouseClicked(MouseEvent evt) {//GEN-FIRST:event_lblClearMouseClicked
        clear();
    }//GEN-LAST:event_lblClearMouseClicked

    private void tbpOutputStateChanged(ChangeEvent evt) {//GEN-FIRST:event_tbpOutputStateChanged
        pnlTabRightButtons.setVisible(tbpOutput.getSelectedIndex() == Amphibia.TAB_PROBLEMS || tbpOutput.getSelectedIndex() == Amphibia.TAB_CONSOLE);
    }//GEN-LAST:event_tbpOutputStateChanged

    private void tbpOutputMouseClicked(MouseEvent evt) {//GEN-FIRST:event_tbpOutputMouseClicked
        Rectangle r = new Rectangle(lblClear.getX(), lblClear.getY(), lblClear.getWidth(), lblClear.getHeight());
        boolean b = r.contains(evt.getPoint());
        if (b) {
            lblClear.dispatchEvent(evt);
        }
    }//GEN-LAST:event_tbpOutputMouseClicked

    private void btnStopActionPerformed(ActionEvent evt) {//GEN-FIRST:event_btnStopActionPerformed
        btnStart.setSelected(false);
        StyledDocument doc = txtServers.getStyledDocument();
        try {
            doc.insertString(doc.getLength(), "Ternimated\n", null);
        } catch (BadLocationException ex) {
            logger.log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_btnStopActionPerformed

    private void btnStartActionPerformed(ActionEvent evt) {//GEN-FIRST:event_btnStartActionPerformed
        txtServers.setText("Started...\n");
    }//GEN-LAST:event_btnStartActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    JToggleButton btnStart;
    JButton btnStop;
    JComboBox<String> cmbServers;
    JScrollPane jScrollPane1;
    JTextArea jTextArea1;
    JLabel lblClear;
    JPanel pnlEditor;
    JPanel pnlInfo;
    JPanel pnlOutput;
    JPanel pnlProfile;
    JPanel pnlServers;
    JPanel pnlServersTop;
    JPanel pnlTabRightButtons;
    JPanel pnlTop;
    JScrollPane splHistory;
    JScrollPane spnConsole;
    JScrollPane spnRaw;
    JScrollPane spnServers;
    JScrollPane srlProblems;
    JTable tblHistory;
    JTabbedPane tbpOutput;
    JTree treeProblems;
    JTextPane txtConsole;
    JTextField txtInfo;
    JEditorPane txtRaw;
    JTextPane txtServers;
    // End of variables declaration//GEN-END:variables

    static public class Entry implements TreeNode {

        public JSON json;
        public String name;
        public Object value;
        public boolean isLeaf;
        public boolean isDelete;
        public Boolean isDynamic;
        public JTreeTable.EditValueRenderer.TYPE type;
        public List<Entry> children;
        private Entry parent;
        public String rootName;

        public Entry(String name) {
            this.name = name;
            this.type = EDIT;
            this.isDelete = false;
            this.children = new ArrayList<>();
        }

        public Entry(Entry parent, JSON json, String name, Object value, boolean isLeaf, boolean isDynamic) {
            this(name);
            this.parent = parent;
            this.json = json;
            this.value = value;
            this.isLeaf = isLeaf;
            this.isDynamic = isDynamic;
        }

        public Object getValue(Object value) {
            if (value instanceof JSONObject && ((JSONObject) value).isNullObject()) {
                return null;
            }
            return value;
        }

        public Entry add(JSON parentJson, String name, Object value, Object propType, Object[] props, Object rootName) {
            boolean isleaf = !(JTreeTable.isNotLeaf(propType) || !(propType instanceof JTreeTable.EditValueRenderer.TYPE));
            Entry entry = new Entry(this, parentJson, name, value, isleaf, false);
            entry.rootName = rootName.toString();
            if (propType instanceof JTreeTable.EditValueRenderer.TYPE) {
                entry.type = (JTreeTable.EditValueRenderer.TYPE) propType;
            } else {
                entry.type = null;
            }
            children.add(entry);

            JSONObject jo = null;
            if (value instanceof JSONObject) {
                jo = (JSONObject) value;
            } else if (value instanceof JSONArray && !(propType instanceof Object[][])) {
                JSONArray list = (JSONArray) value;
                for (int i = 0; i < list.size(); i++) {
                    Entry child = new Entry(entry, list, " ", list.get(i), true, true);
                    if (props != null && props.length == 3) {
                        child.type = (JTreeTable.EditValueRenderer.TYPE) props[2];
                    }
                    entry.children.add(child);
                }
                return entry;
            }

            if (propType instanceof Object[][]) {
                for (Object[] prop : (Object[][]) propType) {
                    String key = prop[0].toString();
                    if (jo != null) {
                        entry.add(jo, key, getValue(jo.get(key)), prop[1], prop, rootName);
                    } else {
                        JSONArray list = (JSONArray) value;
                        for (int i = 0; i < list.size(); i++) {
                            jo = list.getJSONObject(i);
                            entry.add(jo, key, getValue(jo.get(key)), prop[1], prop, rootName);
                        }
                    }
                }
                return entry;
            }

            if (jo != null && !jo.isNullObject()) {
                Iterator<String> keyItr = jo.keys();
                while (keyItr.hasNext()) {
                    String key = keyItr.next();
                    Entry child = new Entry(entry, jo, key, getValue(jo.get(key)), true, true);
                    child.rootName = rootName.toString();
                    if (props.length > 2) {
                        child.type = (JTreeTable.EditValueRenderer.TYPE) props[2];
                    }
                    entry.children.add(child);
                }
            }
            return entry;
        }

        @Override
        public String toString() {
            return name;
        }

        @Override
        public boolean isLeaf() {
            return this.isLeaf;
        }

        @Override
        public TreeNode getChildAt(int childIndex) {
            return children.get(childIndex);
        }

        @Override
        public int getChildCount() {
            return children.size();
        }

        @Override
        public int getIndex(TreeNode node) {
            return children.indexOf(node);
        }

        @Override
        public TreeNode getParent() {
            return parent;
        }

        @Override
        public boolean getAllowsChildren() {
            return true;
        }

        @Override
        public Enumeration children() {
            return Collections.enumeration(children);
        }
    }
}
