/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.equinix.amphibia.components;

import com.equinix.amphibia.Amphibia;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ResourceBundle;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.CellEditor;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

/**
 *
 * @author dgofman
 */
public class Wizard extends javax.swing.JPanel {
    
    private ResourceBundle bundle;
    private JDialog headersDialog;
    private JDialog interfaceDialog;
    private JButton applyInterfaceButton;
    private JButton addInterfaceButton;
    private JButton cancelInterfaceButton;
    private JButton applyHeadersButton;
    private JButton cancelHeadersButton;
    private WizardTab wizardTab;
    private int headerSaveIndex;
    
    MainPanel mainPanel;
    DefaultComboBoxModel interfaceBasePathModel;
   
    private final Object[] headerColumns;
    private final DefaultTableModel headersModel;
    private final DefaultComboBoxModel projectInterfaces;

    public final DefaultComboBoxModel sharedEndPointModel = new DefaultComboBoxModel();

    /**
     * Creates new form Wizard
     */
    public Wizard() {
        bundle = Amphibia.getBundle();

        headerColumns = new String [] {
            bundle.getString("key"),
            bundle.getString("value")
        };
        
        headersModel = new DefaultTableModel();
        projectInterfaces = new DefaultComboBoxModel();
        interfaceBasePathModel = new DefaultComboBoxModel();
        
        initComponents();

        applyInterfaceButton = new JButton(bundle.getString("apply"));
        applyInterfaceButton.addActionListener((ActionEvent evt) -> {
            TreeIconNode node = MainPanel.selectedNode.getCollection().project;
            JSONArray interfaces = new JSONArray();
            for (int i = 0; i < projectInterfaces.getSize(); i++) {
                interfaces.add(((ComboItem)projectInterfaces.getElementAt(i)).json);
            }
            node.jsonObject().element("interfaces", interfaces);
            mainPanel.saveNodeValue(node);
            interfaceDialog.setVisible(false);
        });
        addInterfaceButton = new JButton(bundle.getString("newInterface"));
        addInterfaceButton.addActionListener((ActionEvent evt) -> {
            JSONObject json = new JSONObject();
            json.element("type", "rest");
            if(newInterface(json)) {
                reset();
            }
        });
        cancelInterfaceButton = new JButton(bundle.getString("cancel"));
        cancelInterfaceButton.addActionListener((ActionEvent evt) -> {
            interfaceDialog.setVisible(false);
        });

        interfaceDialog = Amphibia.createDialog(pnlInterface, new Object[]{addInterfaceButton, applyInterfaceButton, cancelInterfaceButton}, true);
        interfaceDialog.setSize(new Dimension(600, 500));

        applyHeadersButton = new JButton(bundle.getString("apply"));
        applyHeadersButton.addActionListener((ActionEvent evt) -> {
            
            headersDialog.setVisible(false);
        });
        cancelHeadersButton = new JButton(bundle.getString("cancel"));
        cancelHeadersButton.addActionListener((ActionEvent evt) -> {
            headersDialog.setVisible(false);
        });
        
        headersDialog = Amphibia.createDialog(pnlHeaders, new Object[]{applyHeadersButton, cancelHeadersButton}, true);
        headersDialog.setSize(new Dimension(600, 700));
    }
    
    public void setMainPanel(MainPanel mainPanel) {
        this.mainPanel = mainPanel;
        EventQueue.invokeLater(() -> {
            interfaceDialog.setLocationRelativeTo(mainPanel);
        });
        EventQueue.invokeLater(() -> {
            headersDialog.setLocationRelativeTo(mainPanel);
        });
    }
    
    public void selectNode(TreeIconNode node) {
        interfaceBasePathModel.removeAllElements();
        projectInterfaces.removeAllElements();
        if (MainPanel.selectedNode != null) {
            JSONObject json = new JSONObject();
            json.element("name", bundle.getString("none"));
            json.element("basePath", "");
            interfaceBasePathModel.addElement(new ComboItem(json));
            json = MainPanel.selectedNode.getCollection().project.jsonObject();
            json.getJSONArray("interfaces").forEach((item) -> {
                ComboItem comboItem = new ComboItem(JSONObject.fromObject(item.toString()));
                interfaceBasePathModel.addElement(comboItem);
                projectInterfaces.addElement(comboItem);
            }); 
        }
    }
    
    private boolean newInterface(JSONObject json) {
        boolean b = saveSelectedModel(cmdName.getSelectedIndex());
        if (b) {
            String[] names = new String[projectInterfaces.getSize()];
            for (int i = 0; i < projectInterfaces.getSize(); i++) {
                names[i] = projectInterfaces.getElementAt(i).toString();
            }
            String name = Amphibia.instance.inputDialog("newInterfaceName", "", names, interfaceDialog.getParent());
            b = name != null && !name.isEmpty();
            if (b) {
                json.element("name", name);
                projectInterfaces.addElement(new ComboItem(json));
            }
            headerSaveIndex = projectInterfaces.getSize() - 1;
            cmdName.setSelectedIndex(headerSaveIndex);
        }
        return b;
    }
    
    private boolean saveSelectedModel(int index) {
        if (index != -1) {
            CellEditor cellEditor = tblEnvHeaders.getCellEditor();
            if (cellEditor != null) {
                cellEditor.stopCellEditing();
            }
            ComboItem item = (ComboItem) projectInterfaces.getElementAt(index);
            JSONObject headers = (JSONObject) item.json.getOrDefault("headers", new JSONObject());
            item.json.element("basePath", txtBasePath.getText());
            headers.clear();
            for (int r = 0; r < headersModel.getRowCount(); r++) {
                Object key = headersModel.getValueAt(r, 0);
                if (key != null && !key.toString().isEmpty()) {
                    if (headers.containsKey(key)) {
                        lblError.setVisible(true);
                        return false;
                    }
                    headers.put(key, headersModel.getValueAt(r, 1));
                }
            }
            item.json.element("headers", headers);
            headerSaveIndex = index;
        }
        return true;
    }

    private void reset() {
        lblError.setVisible(false);
        txtBasePath.setText("/");
        headersModel.setDataVector(new Object [][] {
            {null, null},
            {null, null},
            {null, null},
            {null, null},
            {null, null}
        }, headerColumns);
    }
    
    public void openInterfacePanel() {
        reset();
        cmdNameItemStateChanged(null);
        interfaceDialog.setVisible(true);
    }
    
    public void openHeadersPanel() {
        headersDialog.setVisible(true);
    }
    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        GridBagConstraints gridBagConstraints;

        pnlInterface = new JPanel();
        pnlEnvTop = new JPanel();
        lblName = new JLabel();
        cmdName = new JComboBox<>();
        btnClone = new JButton();
        btnDelete = new JButton();
        lblBasePath = new JLabel();
        txtBasePath = new JTextField();
        pnlEnvCenter = new JPanel();
        lblEnvHeaders = new JLabel();
        spnEnvHeaders = new JScrollPane();
        tblEnvHeaders = new JTable();
        pnlEnvFooter = new JPanel();
        btnAddRow = new JButton();
        btnDeleteRow = new JButton();
        lblError = new JLabel();
        pnlHeaders = new JPanel();
        sptHeaders = new JSplitPane();
        pnlEnvCenter1 = new JPanel();
        lblEnvHeaders1 = new JLabel();
        spnEnvHeaders1 = new JScrollPane();
        tblEnvHeaders1 = new JTable();
        pnlEnvFooter1 = new JPanel();
        pnlEnvCenter2 = new JPanel();
        lblEnvHeaders2 = new JLabel();
        spnEnvHeaders2 = new JScrollPane();
        tblEnvHeaders2 = new JTable();
        pnlEnvFooter2 = new JPanel();
        btnAddRow2 = new JButton();
        btnDeleteRow2 = new JButton();
        lblError2 = new JLabel();
        tabNav = new JTabbedPane();

        pnlInterface.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        pnlInterface.setLayout(new BorderLayout());

        pnlEnvTop.setLayout(new GridBagLayout());

        ResourceBundle bundle = ResourceBundle.getBundle("com/equinix/amphibia/messages"); // NOI18N
        lblName.setText(bundle.getString("interfaceName")); // NOI18N
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        pnlEnvTop.add(lblName, gridBagConstraints);

        cmdName.setModel(projectInterfaces);
        cmdName.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent evt) {
                cmdNameItemStateChanged(evt);
            }
        });
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new Insets(0, 5, 0, 5);
        pnlEnvTop.add(cmdName, gridBagConstraints);

        btnClone.setIcon(new ImageIcon(getClass().getResource("/com/equinix/amphibia/icons/clone_16.png"))); // NOI18N
        btnClone.setToolTipText(bundle.getString("clone")); // NOI18N
        btnClone.setMargin(new Insets(2, 2, 2, 2));
        btnClone.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                btnCloneActionPerformed(evt);
            }
        });
        pnlEnvTop.add(btnClone, new GridBagConstraints());

        btnDelete.setIcon(new ImageIcon(getClass().getResource("/com/equinix/amphibia/icons/close_16.png"))); // NOI18N
        btnDelete.setToolTipText(bundle.getString("delete")); // NOI18N
        btnDelete.setMargin(new Insets(2, 2, 2, 2));
        btnDelete.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                btnDeleteActionPerformed(evt);
            }
        });
        pnlEnvTop.add(btnDelete, new GridBagConstraints());

        lblBasePath.setText(bundle.getString("basePath")); // NOI18N
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        pnlEnvTop.add(lblBasePath, gridBagConstraints);

        txtBasePath.setText("/");
        txtBasePath.setToolTipText("");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        pnlEnvTop.add(txtBasePath, gridBagConstraints);

        pnlInterface.add(pnlEnvTop, BorderLayout.NORTH);

        pnlEnvCenter.setLayout(new BorderLayout());

        lblEnvHeaders.setFont(new Font("Tahoma", 1, 11)); // NOI18N
        lblEnvHeaders.setText(bundle.getString("headers")); // NOI18N
        lblEnvHeaders.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
        pnlEnvCenter.add(lblEnvHeaders, BorderLayout.PAGE_START);

        tblEnvHeaders.setModel(this.headersModel);
        spnEnvHeaders.setViewportView(tblEnvHeaders);

        pnlEnvCenter.add(spnEnvHeaders, BorderLayout.CENTER);

        pnlEnvFooter.setLayout(new BoxLayout(pnlEnvFooter, BoxLayout.LINE_AXIS));

        btnAddRow.setIcon(new ImageIcon(getClass().getResource("/com/equinix/amphibia/icons/plus-icon.png"))); // NOI18N
        btnAddRow.setToolTipText(bundle.getString("addRow")); // NOI18N
        btnAddRow.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                btnAddRowActionPerformed(evt);
            }
        });
        pnlEnvFooter.add(btnAddRow);

        btnDeleteRow.setIcon(new ImageIcon(getClass().getResource("/com/equinix/amphibia/icons/remove_16.png"))); // NOI18N
        btnDeleteRow.setToolTipText(bundle.getString("deleteRow")); // NOI18N
        btnDeleteRow.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                btnDeleteRowActionPerformed(evt);
            }
        });
        pnlEnvFooter.add(btnDeleteRow);

        lblError.setForeground(Color.red);
        lblError.setText(bundle.getString("tip_key_exists")); // NOI18N
        lblError.setBorder(BorderFactory.createEmptyBorder(1, 10, 1, 10));
        pnlEnvFooter.add(lblError);

        pnlEnvCenter.add(pnlEnvFooter, BorderLayout.PAGE_END);

        pnlInterface.add(pnlEnvCenter, BorderLayout.CENTER);

        pnlHeaders.setLayout(new BorderLayout());

        sptHeaders.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
        sptHeaders.setDividerLocation(150);
        sptHeaders.setDividerSize(3);
        sptHeaders.setOrientation(JSplitPane.VERTICAL_SPLIT);

        pnlEnvCenter1.setLayout(new BorderLayout());

        lblEnvHeaders1.setFont(new Font("Tahoma", 1, 11)); // NOI18N
        lblEnvHeaders1.setText(bundle.getString("sharedHeaders")); // NOI18N
        lblEnvHeaders1.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
        pnlEnvCenter1.add(lblEnvHeaders1, BorderLayout.PAGE_START);

        tblEnvHeaders1.setModel(this.headersModel);
        tblEnvHeaders1.setEnabled(false);
        spnEnvHeaders1.setViewportView(tblEnvHeaders1);

        pnlEnvCenter1.add(spnEnvHeaders1, BorderLayout.CENTER);

        pnlEnvFooter1.setLayout(new BoxLayout(pnlEnvFooter1, BoxLayout.LINE_AXIS));
        pnlEnvCenter1.add(pnlEnvFooter1, BorderLayout.PAGE_END);

        sptHeaders.setLeftComponent(pnlEnvCenter1);

        pnlEnvCenter2.setLayout(new BorderLayout());

        lblEnvHeaders2.setFont(new Font("Tahoma", 1, 11)); // NOI18N
        lblEnvHeaders2.setText(bundle.getString("headers")); // NOI18N
        lblEnvHeaders2.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
        pnlEnvCenter2.add(lblEnvHeaders2, BorderLayout.PAGE_START);

        tblEnvHeaders2.setModel(this.headersModel);
        spnEnvHeaders2.setViewportView(tblEnvHeaders2);

        pnlEnvCenter2.add(spnEnvHeaders2, BorderLayout.CENTER);

        pnlEnvFooter2.setLayout(new BoxLayout(pnlEnvFooter2, BoxLayout.LINE_AXIS));

        btnAddRow2.setIcon(new ImageIcon(getClass().getResource("/com/equinix/amphibia/icons/plus-icon.png"))); // NOI18N
        btnAddRow2.setToolTipText(bundle.getString("addRow")); // NOI18N
        btnAddRow2.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                btnAddRow2ActionPerformed(evt);
            }
        });
        pnlEnvFooter2.add(btnAddRow2);

        btnDeleteRow2.setIcon(new ImageIcon(getClass().getResource("/com/equinix/amphibia/icons/remove_16.png"))); // NOI18N
        btnDeleteRow2.setToolTipText(bundle.getString("deleteRow")); // NOI18N
        btnDeleteRow2.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                btnDeleteRow2ActionPerformed(evt);
            }
        });
        pnlEnvFooter2.add(btnDeleteRow2);

        lblError2.setForeground(Color.red);
        lblError2.setText(bundle.getString("tip_key_exists")); // NOI18N
        lblError2.setBorder(BorderFactory.createEmptyBorder(1, 10, 1, 10));
        pnlEnvFooter2.add(lblError2);

        pnlEnvCenter2.add(pnlEnvFooter2, BorderLayout.PAGE_END);

        sptHeaders.setRightComponent(pnlEnvCenter2);

        pnlHeaders.add(sptHeaders, BorderLayout.CENTER);

        setBorder(BorderFactory.createLineBorder(new Color(0, 0, 0)));
        setLayout(new BorderLayout());

        wizardTab = new WizardTab(this);
        tabNav.addTab("",
            new ImageIcon(getClass().getResource("/com/equinix/amphibia/icons/wizard_16.png")),
            wizardTab);
        add(tabNav, BorderLayout.CENTER);
    }// </editor-fold>//GEN-END:initComponents

    private void btnCloneActionPerformed(ActionEvent evt) {//GEN-FIRST:event_btnCloneActionPerformed
        if (cmdName.getSelectedIndex() != -1) {
            if (saveSelectedModel(headerSaveIndex)) {
                ComboItem item = (ComboItem) projectInterfaces.getElementAt(cmdName.getSelectedIndex());
                newInterface(JSONObject.fromObject(item.json.toString()));
            }
        }
    }//GEN-LAST:event_btnCloneActionPerformed

    private void btnAddRowActionPerformed(ActionEvent evt) {//GEN-FIRST:event_btnAddRowActionPerformed
        int row = tblEnvHeaders.getSelectedRow();
        if (row == -1) {
            headersModel.addRow(new Object[]{null, null});
        } else {
            headersModel.insertRow(row + 1, new Object[]{null, null});
        }
    }//GEN-LAST:event_btnAddRowActionPerformed

    private void btnDeleteRowActionPerformed(ActionEvent evt) {//GEN-FIRST:event_btnDeleteRowActionPerformed
        if (headersModel.getRowCount() > 0) {
            int row = tblEnvHeaders.getSelectedRow();
            if (row == -1) {
                headersModel.removeRow(headersModel.getRowCount() - 1);
            } else {
                headersModel.removeRow(row);
            }
        }
    }//GEN-LAST:event_btnDeleteRowActionPerformed

    private void btnDeleteActionPerformed(ActionEvent evt) {//GEN-FIRST:event_btnDeleteActionPerformed
        int n = JOptionPane.showConfirmDialog(interfaceDialog,
                    String.format(bundle.getString("tip_delete_interface"), cmdName.getSelectedItem()), bundle.getString("title"),
                    JOptionPane.YES_NO_OPTION);
            if (n == JOptionPane.YES_OPTION) {
                projectInterfaces.removeElementAt(cmdName.getSelectedIndex());
            }
            headerSaveIndex = -1;
            cmdNameItemStateChanged(null);
    }//GEN-LAST:event_btnDeleteActionPerformed

    private void cmdNameItemStateChanged(ItemEvent evt) {//GEN-FIRST:event_cmdNameItemStateChanged
        int size = projectInterfaces.getSize();
        btnClone.setEnabled(size > 0);
        btnDelete.setEnabled(size > 0);
        if (size > 0) {
            if (evt != null && evt.getStateChange() == ItemEvent.DESELECTED) {
                if (saveSelectedModel(headerSaveIndex)) {
                    reset();
                }
            } else {
                ComboItem item = (ComboItem) projectInterfaces.getElementAt(cmdName.getSelectedIndex());
                txtBasePath.setText(item.json.getOrDefault("basePath", "/").toString());
                if (item.json.containsKey("headers")) {
                    JSONObject headers = item.json.getJSONObject("headers");
                    if (!headers.isEmpty()) {
                        headersModel.setRowCount(0);
                        headers.keySet().forEach((key) -> {
                            headersModel.addRow(new Object[] {key, headers.get(key)});
                        });
                    }
                }
                headerSaveIndex = cmdName.getSelectedIndex();
            }
        }
    }//GEN-LAST:event_cmdNameItemStateChanged

    private void btnAddRow2ActionPerformed(ActionEvent evt) {//GEN-FIRST:event_btnAddRow2ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_btnAddRow2ActionPerformed

    private void btnDeleteRow2ActionPerformed(ActionEvent evt) {//GEN-FIRST:event_btnDeleteRow2ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_btnDeleteRow2ActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private JButton btnAddRow;
    private JButton btnAddRow2;
    private JButton btnClone;
    private JButton btnDelete;
    private JButton btnDeleteRow;
    private JButton btnDeleteRow2;
    private JComboBox<String> cmdName;
    private JLabel lblBasePath;
    private JLabel lblEnvHeaders;
    private JLabel lblEnvHeaders1;
    private JLabel lblEnvHeaders2;
    private JLabel lblError;
    private JLabel lblError2;
    private JLabel lblName;
    private JPanel pnlEnvCenter;
    private JPanel pnlEnvCenter1;
    private JPanel pnlEnvCenter2;
    private JPanel pnlEnvFooter;
    private JPanel pnlEnvFooter1;
    private JPanel pnlEnvFooter2;
    private JPanel pnlEnvTop;
    private JPanel pnlHeaders;
    private JPanel pnlInterface;
    private JScrollPane spnEnvHeaders;
    private JScrollPane spnEnvHeaders1;
    private JScrollPane spnEnvHeaders2;
    private JSplitPane sptHeaders;
    JTabbedPane tabNav;
    private JTable tblEnvHeaders;
    private JTable tblEnvHeaders1;
    private JTable tblEnvHeaders2;
    private JTextField txtBasePath;
    // End of variables declaration//GEN-END:variables


    class ComboItem {
        String label;
        JSONObject json;
        
        public ComboItem(JSONObject item) {
            this.json = item;
            this.label = item.getString("name");
        }
        
        @Override
        public String toString() {
            return label;
        }
    }
}
