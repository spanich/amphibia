/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.equinix.amphibia.components;

import com.equinix.amphibia.Amphibia;
import com.equinix.amphibia.IO;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.CellEditor;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

/**
 *
 * @author dgofman
 */
public final class WizardTab extends javax.swing.JPanel {
    
    private ResourceBundle bundle;
    private Wizard wizard;
    private TreeIconNode testCaseNode;
    private JDialog headersDialog;
    private JButton applyHeadersButton;
    private JButton cancelHeadersButton;
    private DefaultTableModel testCaseHeaders;
    private Object[][] lastSavedDataModel;

    private static final Logger logger = Logger.getLogger(WizardTab.class.getName());
    
    /**
     * Creates new form WizardTab
     * @param wizard the value of wizard
     */
    public WizardTab(Wizard wizard) {
        this(wizard, null);
    }
    
    public WizardTab(Wizard wizard, TreeIconNode node) {
        this.wizard = wizard;
        this.testCaseNode = node;
        
        bundle = Amphibia.getBundle();
        
        lastSavedDataModel = new Object[][]{};
        testCaseHeaders = new DefaultTableModel(lastSavedDataModel, wizard.headerColumns);
        
        initComponents();
        
        if (testCaseNode != null) {
            cmdInterface.setEnabled(false);
            cmdMethod.setEnabled(false);
            String method = node.jsonObject().getString("method");
            for (int i = 0; i < cmdMethod.getItemCount(); i++) {
                if (cmdMethod.getItemAt(i).equals(method)) {
                    cmdMethod.setSelectedIndex(i);
                    break;
                }
            }
            
            txtPath.setText(node.jsonObject().getString("reqPath"));
            
            JSONObject request = node.info.testStepInfo.getJSONObject("request");
            String json = null;
            if (request.get("body") instanceof String) {
                json = request.getString("body");
                try {
                    json = IO.readFile(node.getCollection(), json);
                    json = IO.prettyJson(json);
                    json = node.info.properties.cloneProperties().setTestStep(request.getJSONObject("properties")).replace(json);
                } catch (Exception ex) {
                    logger.log(Level.SEVERE, null, ex);
                }
            }
            txtReqBody.setText(json);
        } else {
            btnClose.setVisible(false);
        }
        
        updateInterfaces();
        
        applyHeadersButton = new JButton(bundle.getString("apply"));
        applyHeadersButton.addActionListener((ActionEvent evt) -> {
            CellEditor cellEditor = tblHeadersBottom.getCellEditor();
            if (cellEditor != null) {
                cellEditor.stopCellEditing();
            }
            Map<Object, Object[]> map = new HashMap<>();
            for (int r = 0; r < tblHeadersBottom.getRowCount(); r++) {
                Object key = tblHeadersBottom.getValueAt(r, 0);
                if (key != null && !key.toString().isEmpty()) {
                    if (map.containsKey(key)) {
                        lblHeadersBottomError.setVisible(true);
                        return;
                    }
                    map.put(key, new Object[] {key, tblHeadersBottom.getValueAt(r, 1)});
                }
            }
            lastSavedDataModel = map.values().toArray(new Object[map.size()][2]);
            headersDialog.setVisible(false);
        });
        cancelHeadersButton = new JButton(bundle.getString("cancel"));
        cancelHeadersButton.addActionListener((ActionEvent evt) -> {
            headersDialog.setVisible(false);
        });
        
        headersDialog = Amphibia.createDialog(pnlHeaders, new Object[]{applyHeadersButton, cancelHeadersButton}, true);
        headersDialog.setSize(new Dimension(500, 500));
        
        EventQueue.invokeLater(() -> {
            headersDialog.setLocationRelativeTo(this);
        });
    }
    
    public void refresh() {
        btnSend.setEnabled(cmdEndpoint.getSelectedItem() != null);
        if (btnSend.isEnabled()) {
            String basePath = "/";
            if (cmdInterface.getSelectedItem() != null) {
                basePath = ((Wizard.ComboItem)cmdInterface.getSelectedItem()).json.getString("basePath");
            }
            lblURI.setText(cmdEndpoint.getSelectedItem().toString() + basePath + 
                    (txtPath.getText().startsWith("/")  ? "" : "/") + txtPath.getText());
        } else {
            lblURI.setText("http://");
        }
    }
    
    public void updateInterfaces() {
        String selected = String.valueOf(cmdInterface.getSelectedItem());
        DefaultComboBoxModel model = new DefaultComboBoxModel();
        cmdInterface.setModel(model);
        model.addElement(wizard.createDefaultItem());
        if (testCaseNode != null) {
            String interfaceName = testCaseNode.info.resource.getString("interface");
            if (!interfaceName.isEmpty()) {
                JSONArray interfaces = testCaseNode.getCollection().interfaces.jsonArray();
                for (int i = 0; i < interfaces.size(); i++) {
                    JSONObject interf = interfaces.getJSONObject(i);
                    if (interf.getString("name").equals(interfaceName)) {
                        model.removeAllElements();
                        model.addElement(new Wizard.ComboItem(interf, false));
                        break;
                    }
                }
            }
        } else {
            DefaultComboBoxModel interfaceModel = wizard.getInterfaceNameModel();
            for (int i = 0; i < interfaceModel.getSize(); i++) {
                Wizard.ComboItem item = (Wizard.ComboItem)interfaceModel.getElementAt(i);
                model.addElement(item);
                if (item.toString().equals(selected)) {
                    cmdInterface.setSelectedItem(item);
                }
            }
        }
    }
    
    public void updateEndPoints(String[] endpoints) {
        Object selected = cmdEndpoint.getSelectedItem();
        cmdEndpoint.setModel(new DefaultComboBoxModel<>(endpoints));
        cmdEndpoint.setSelectedItem(selected);
        if (cmdEndpoint.getSelectedIndex() == -1) {
            cmdEndpoint.setSelectedIndex(0);
        }
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

        pnlHeaders = new JPanel();
        sptHeaders = new JSplitPane();
        pnlHeadersTop = new JPanel();
        lblHeadersTop = new JLabel();
        spnHeadersTop = new JScrollPane();
        tblHeadersTop = new JTable();
        pnlHeadersBottom = new JPanel();
        pnlHeaderBottomHeader = new JPanel();
        lblHeadersBottom = new JLabel();
        lblHeaderBottomHint = new JLabel();
        spnHeadersBottom = new JScrollPane();
        tblHeadersBottom = new JTable();
        pnlHeadersBottomFooter = new JPanel();
        btnHeadersBottomAdd = new JButton();
        btnHeadersBottomDelete = new JButton();
        lblHeadersBottomError = new JLabel();
        pnlTop = new JPanel();
        lblEndpoint = new JLabel();
        pnlEndpoint = new JPanel();
        cmdEndpoint = new JComboBox<>();
        btnEndpointInfo = new JButton();
        lblInterface = new JLabel();
        pnlInterface = new JPanel();
        cmdInterface = new JComboBox<>();
        btnInterfaceInfo = new JButton();
        lblMethod = new JLabel();
        pnlMethodURI = new JPanel();
        cmdMethod = new JComboBox<>();
        lblURI = new JLabel();
        lblPath = new JLabel();
        txtPath = new JTextField();
        btnHeaders = new JButton();
        btnClose = new JButton();
        tabBody = new JTabbedPane();
        spnReqBody = new JScrollPane();
        txtReqBody = new JTextArea();
        spnResBody = new JScrollPane();
        txtResBody = new JTextArea();
        spnConsole = new JScrollPane();
        txtConsole = new JTextArea();
        pnlFooter = new JPanel();
        lblStatusCode = new JLabel();
        lblCode = new JLabel();
        btnSend = new JButton();
        btnSave = new JButton();

        pnlHeaders.setLayout(new BorderLayout());

        sptHeaders.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
        sptHeaders.setDividerLocation(150);
        sptHeaders.setDividerSize(3);
        sptHeaders.setOrientation(JSplitPane.VERTICAL_SPLIT);

        pnlHeadersTop.setLayout(new BorderLayout());

        lblHeadersTop.setFont(new Font("Tahoma", 1, 11)); // NOI18N
        ResourceBundle bundle = ResourceBundle.getBundle("com/equinix/amphibia/messages"); // NOI18N
        lblHeadersTop.setText(bundle.getString("sharedHeaders")); // NOI18N
        lblHeadersTop.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
        pnlHeadersTop.add(lblHeadersTop, BorderLayout.PAGE_START);

        tblHeadersTop.setModel(new DefaultTableModel(
            new Object [][] {

            },
            new String [] {

            }
        ));
        tblHeadersTop.setEnabled(false);
        spnHeadersTop.setViewportView(tblHeadersTop);

        pnlHeadersTop.add(spnHeadersTop, BorderLayout.CENTER);

        sptHeaders.setLeftComponent(pnlHeadersTop);

        pnlHeadersBottom.setLayout(new BorderLayout());

        pnlHeaderBottomHeader.setLayout(new BorderLayout());

        lblHeadersBottom.setFont(new Font("Tahoma", 1, 11)); // NOI18N
        lblHeadersBottom.setText(bundle.getString("headers")); // NOI18N
        lblHeadersBottom.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
        pnlHeaderBottomHeader.add(lblHeadersBottom, BorderLayout.WEST);

        lblHeaderBottomHint.setForeground(Color.blue);
        lblHeaderBottomHint.setText(bundle.getString("tip_edit_column")); // NOI18N
        pnlHeaderBottomHeader.add(lblHeaderBottomHint, BorderLayout.EAST);

        pnlHeadersBottom.add(pnlHeaderBottomHeader, BorderLayout.NORTH);

        tblHeadersBottom.setModel(testCaseHeaders);
        spnHeadersBottom.setViewportView(tblHeadersBottom);

        pnlHeadersBottom.add(spnHeadersBottom, BorderLayout.CENTER);

        pnlHeadersBottomFooter.setLayout(new BoxLayout(pnlHeadersBottomFooter, BoxLayout.LINE_AXIS));

        btnHeadersBottomAdd.setIcon(new ImageIcon(getClass().getResource("/com/equinix/amphibia/icons/plus-icon.png"))); // NOI18N
        btnHeadersBottomAdd.setToolTipText(bundle.getString("addRow")); // NOI18N
        btnHeadersBottomAdd.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                btnHeadersBottomAddActionPerformed(evt);
            }
        });
        pnlHeadersBottomFooter.add(btnHeadersBottomAdd);

        btnHeadersBottomDelete.setIcon(new ImageIcon(getClass().getResource("/com/equinix/amphibia/icons/remove_16.png"))); // NOI18N
        btnHeadersBottomDelete.setToolTipText(bundle.getString("deleteRow")); // NOI18N
        btnHeadersBottomDelete.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                btnHeadersBottomDeleteActionPerformed(evt);
            }
        });
        pnlHeadersBottomFooter.add(btnHeadersBottomDelete);

        lblHeadersBottomError.setForeground(Color.red);
        lblHeadersBottomError.setText(bundle.getString("tip_key_exists")); // NOI18N
        lblHeadersBottomError.setBorder(BorderFactory.createEmptyBorder(1, 10, 1, 10));
        pnlHeadersBottomFooter.add(lblHeadersBottomError);

        pnlHeadersBottom.add(pnlHeadersBottomFooter, BorderLayout.PAGE_END);

        sptHeaders.setRightComponent(pnlHeadersBottom);

        pnlHeaders.add(sptHeaders, BorderLayout.CENTER);

        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        setLayout(new BorderLayout());

        pnlTop.setLayout(new GridBagLayout());

        lblEndpoint.setText(bundle.getString("endpoint")); // NOI18N
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        pnlTop.add(lblEndpoint, gridBagConstraints);

        pnlEndpoint.setLayout(new FlowLayout(FlowLayout.LEFT));

        cmdEndpoint.setPreferredSize(new Dimension(250, 20));
        cmdEndpoint.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent evt) {
                cmdEndpointItemStateChanged(evt);
            }
        });
        pnlEndpoint.add(cmdEndpoint);

        btnEndpointInfo.setIcon(new ImageIcon(getClass().getResource("/com/equinix/amphibia/icons/icon-16-info.png"))); // NOI18N
        btnEndpointInfo.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnEndpointInfo.setFocusPainted(false);
        btnEndpointInfo.setPreferredSize(new Dimension(30, 22));
        btnEndpointInfo.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                btnEndpointInfoActionPerformed(evt);
            }
        });
        pnlEndpoint.add(btnEndpointInfo);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        pnlTop.add(pnlEndpoint, gridBagConstraints);

        lblInterface.setText(bundle.getString("interface")); // NOI18N
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        pnlTop.add(lblInterface, gridBagConstraints);

        pnlInterface.setLayout(new FlowLayout(FlowLayout.LEFT));

        cmdInterface.setPreferredSize(new Dimension(250, 20));
        cmdInterface.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent evt) {
                cmdInterfaceItemStateChanged(evt);
            }
        });
        pnlInterface.add(cmdInterface);

        btnInterfaceInfo.setIcon(new ImageIcon(getClass().getResource("/com/equinix/amphibia/icons/icon-16-info.png"))); // NOI18N
        btnInterfaceInfo.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnInterfaceInfo.setFocusPainted(false);
        btnInterfaceInfo.setPreferredSize(new Dimension(30, 22));
        btnInterfaceInfo.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                btnInterfaceInfoActionPerformed(evt);
            }
        });
        pnlInterface.add(btnInterfaceInfo);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        pnlTop.add(pnlInterface, gridBagConstraints);

        lblMethod.setText(bundle.getString("method")); // NOI18N
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        pnlTop.add(lblMethod, gridBagConstraints);

        pnlMethodURI.setLayout(new BorderLayout(15, 0));

        cmdMethod.setModel(new DefaultComboBoxModel<>(new String[] { "GET", "POST", "DELETE", "PUT", "PATCH", "OPTIONS" }));
        pnlMethodURI.add(cmdMethod, BorderLayout.WEST);

        lblURI.setText("http://");
        pnlMethodURI.add(lblURI, BorderLayout.CENTER);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        pnlTop.add(pnlMethodURI, gridBagConstraints);

        lblPath.setText(bundle.getString("path")); // NOI18N
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        pnlTop.add(lblPath, gridBagConstraints);

        txtPath.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent evt) {
                txtPathKeyReleased(evt);
            }
        });
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        pnlTop.add(txtPath, gridBagConstraints);

        btnHeaders.setText(bundle.getString("headers")); // NOI18N
        btnHeaders.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                btnHeadersActionPerformed(evt);
            }
        });
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridy = 4;
        pnlTop.add(btnHeaders, gridBagConstraints);

        btnClose.setIcon(new ImageIcon(getClass().getResource("/com/equinix/amphibia/icons/close_16.png"))); // NOI18N
        btnClose.setToolTipText(bundle.getString("close")); // NOI18N
        btnClose.setMargin(new Insets(2, 2, 2, 2));
        btnClose.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                btnCloseActionPerformed(evt);
            }
        });
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.anchor = GridBagConstraints.EAST;
        pnlTop.add(btnClose, gridBagConstraints);

        add(pnlTop, BorderLayout.NORTH);

        txtReqBody.setColumns(20);
        txtReqBody.setRows(5);
        spnReqBody.setViewportView(txtReqBody);

        tabBody.addTab(bundle.getString("requestBody"), spnReqBody); // NOI18N

        txtResBody.setColumns(20);
        txtResBody.setRows(5);
        spnResBody.setViewportView(txtResBody);

        tabBody.addTab(bundle.getString("responseBody"), spnResBody); // NOI18N

        txtConsole.setEditable(false);
        txtConsole.setColumns(20);
        txtConsole.setRows(5);
        spnConsole.setViewportView(txtConsole);

        tabBody.addTab(bundle.getString("console"), spnConsole); // NOI18N

        add(tabBody, BorderLayout.CENTER);

        pnlFooter.setLayout(new GridBagLayout());

        lblStatusCode.setText(bundle.getString("statusCode")); // NOI18N
        pnlFooter.add(lblStatusCode, new GridBagConstraints());

        lblCode.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        pnlFooter.add(lblCode, gridBagConstraints);

        btnSend.setText(bundle.getString("send")); // NOI18N
        btnSend.setEnabled(false);
        btnSend.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                btnSendActionPerformed(evt);
            }
        });
        pnlFooter.add(btnSend, new GridBagConstraints());

        btnSave.setText(bundle.getString("save")); // NOI18N
        btnSave.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                btnSaveActionPerformed(evt);
            }
        });
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        pnlFooter.add(btnSave, gridBagConstraints);

        add(pnlFooter, BorderLayout.SOUTH);
    }// </editor-fold>//GEN-END:initComponents

    private void btnEndpointInfoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnEndpointInfoActionPerformed
        wizard.mainPanel.globalVarsDialog.openDialog();
        Amphibia.instance.resetEnvironmentModel();
    }//GEN-LAST:event_btnEndpointInfoActionPerformed

    private void btnHeadersActionPerformed(ActionEvent evt) {//GEN-FIRST:event_btnHeadersActionPerformed
        lblHeadersBottomError.setVisible(false);
        Wizard.ComboItem item = (Wizard.ComboItem) cmdInterface.getSelectedItem();
        JSONObject headers = (JSONObject) item.json.getOrDefault("headers", new JSONObject());
        Object[][] sharedHeaders = new Object[headers.size()][2];
        int row = 0;
        for (Object key : headers.keySet()) {
            sharedHeaders[row][0] = key;
            sharedHeaders[row][1] = headers.get(key);
            row++;
        }
        tblHeadersTop.setModel(new DefaultTableModel(sharedHeaders, wizard.headerColumns));
        testCaseHeaders.setDataVector(lastSavedDataModel, wizard.headerColumns);
        headersDialog.setVisible(true);
    }//GEN-LAST:event_btnHeadersActionPerformed

    private void btnSendActionPerformed(ActionEvent evt) {//GEN-FIRST:event_btnSendActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_btnSendActionPerformed

    private void btnSaveActionPerformed(ActionEvent evt) {//GEN-FIRST:event_btnSaveActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_btnSaveActionPerformed

    private void btnInterfaceInfoActionPerformed(ActionEvent evt) {//GEN-FIRST:event_btnInterfaceInfoActionPerformed
        wizard.openInterfacePanel((Wizard.ComboItem)cmdInterface.getSelectedItem());
    }//GEN-LAST:event_btnInterfaceInfoActionPerformed

    private void cmdEndpointItemStateChanged(ItemEvent evt) {//GEN-FIRST:event_cmdEndpointItemStateChanged
        refresh();
    }//GEN-LAST:event_cmdEndpointItemStateChanged

    private void cmdInterfaceItemStateChanged(ItemEvent evt) {//GEN-FIRST:event_cmdInterfaceItemStateChanged
        refresh();
    }//GEN-LAST:event_cmdInterfaceItemStateChanged

    private void btnHeadersBottomAddActionPerformed(ActionEvent evt) {//GEN-FIRST:event_btnHeadersBottomAddActionPerformed
        testCaseHeaders.addRow(new Object[][]{null, null});
    }//GEN-LAST:event_btnHeadersBottomAddActionPerformed

    private void btnHeadersBottomDeleteActionPerformed(ActionEvent evt) {//GEN-FIRST:event_btnHeadersBottomDeleteActionPerformed
        if (testCaseHeaders.getRowCount() > 0) {
            int index = tblHeadersBottom.getSelectedRow();
            testCaseHeaders.removeRow(index == -1 ? testCaseHeaders.getRowCount() - 1 : index);
        }
    }//GEN-LAST:event_btnHeadersBottomDeleteActionPerformed

    private void txtPathKeyReleased(KeyEvent evt) {//GEN-FIRST:event_txtPathKeyReleased
        refresh();
    }//GEN-LAST:event_txtPathKeyReleased

    private void btnCloseActionPerformed(ActionEvent evt) {//GEN-FIRST:event_btnCloseActionPerformed
        for (int i = 0; i < wizard.tabNav.getTabCount(); i++) {
            if (wizard.tabNav.getComponent(i) == this) {
                wizard.tabNav.remove(i);
                break;
            }
        }
    }//GEN-LAST:event_btnCloseActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    JButton btnClose;
    JButton btnEndpointInfo;
    JButton btnHeaders;
    JButton btnHeadersBottomAdd;
    JButton btnHeadersBottomDelete;
    JButton btnInterfaceInfo;
    JButton btnSave;
    JButton btnSend;
    JComboBox<String> cmdEndpoint;
    JComboBox<String> cmdInterface;
    JComboBox<String> cmdMethod;
    JLabel lblCode;
    JLabel lblEndpoint;
    JLabel lblHeaderBottomHint;
    JLabel lblHeadersBottom;
    JLabel lblHeadersBottomError;
    JLabel lblHeadersTop;
    JLabel lblInterface;
    JLabel lblMethod;
    JLabel lblPath;
    JLabel lblStatusCode;
    JLabel lblURI;
    JPanel pnlEndpoint;
    JPanel pnlFooter;
    JPanel pnlHeaderBottomHeader;
    JPanel pnlHeaders;
    JPanel pnlHeadersBottom;
    JPanel pnlHeadersBottomFooter;
    JPanel pnlHeadersTop;
    JPanel pnlInterface;
    JPanel pnlMethodURI;
    JPanel pnlTop;
    JScrollPane spnConsole;
    JScrollPane spnHeadersBottom;
    JScrollPane spnHeadersTop;
    JScrollPane spnReqBody;
    JScrollPane spnResBody;
    JSplitPane sptHeaders;
    JTabbedPane tabBody;
    JTable tblHeadersBottom;
    JTable tblHeadersTop;
    JTextArea txtConsole;
    JTextField txtPath;
    JTextArea txtReqBody;
    JTextArea txtResBody;
    // End of variables declaration//GEN-END:variables
}
