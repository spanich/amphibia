/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.equinix.amphibia.components;

import static com.equinix.amphibia.components.JTreeTable.EditValueRenderer.TYPE.*;

import com.equinix.amphibia.Amphibia;
import com.equinix.amphibia.IO;

import java.awt.BorderLayout;
import java.awt.Color;
import java.text.NumberFormat;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Enumeration;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.Border;
import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONNull;
import net.sf.json.JSONObject;

/**
 *
 * @author dgofman
 */
public final class ResourceEditDialog extends javax.swing.JPanel {

    private MainPanel mainPanel;
    private JOptionPane optionPane;
    private JDialog dialog;
    private JButton applyButton;
    private JButton deleteButton;
    private JButton cancelButton;
    private JButton okButton;
    private ResourceBundle bundle;
    private Editor.Entry entry;
    private Border defaultBorder;
    
    private final Border ERROR_BORDER = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.RED),
            BorderFactory.createEmptyBorder(2, 2, 2, 2));

    private static final Logger logger = Logger.getLogger(ResourceEditDialog.class.getName());
    private static final NumberFormat NUMBER = NumberFormat.getInstance();

    /**
     * Creates new form TableEditDialog
     *
     * @param mainPanel
     */
    public ResourceEditDialog(MainPanel mainPanel) {
        this.mainPanel = mainPanel;

        initComponents();

        bundle = Amphibia.getBundle();

        defaultBorder = txtName.getBorder();
        applyButton = new JButton(bundle.getString("apply"));
        applyButton.addActionListener((ActionEvent evt) -> {
            entry.isDelete = false;
            if (txtName.getText().isEmpty()) {
                txtName.setBorder(ERROR_BORDER);
                return;
            }
            String dataType = cmbDataType.getSelectedItem().toString();
            try {
                Object value = getValue(dataType, txtEditor.getText().trim());
                if (value == null) {
                    value = JSONNull.getInstance();
                }
                TreeIconNode node = MainPanel.selectedNode;
                TreeCollection collection = node.getCollection();
                TreeCollection.TYPE type = node.getType();
                if ("name".equals(entry.name)) {
                    value = value.toString().trim();
                    if (type == TreeCollection.TYPE.INTERFACE) {
                        JSONObject json = collection.project.jsonObject();
                        JSONArray interfaces = json.getJSONArray("interfaces");
                        String currentName = ((JSONObject)entry.json).getString("name");
                        for (Object item : interfaces) {
                            JSONObject itf = (JSONObject) item;
                            if (itf.getString("name").equals(value) && !itf.toString().equals(entry.json.toString())) {
                                lblError.setText(String.format(bundle.getString("tip_name_exists")));
                                lblError.setVisible(true);
                                return;
                            }
                            if (currentName.equals(itf.getString("name"))) {
                                itf.element("name", value);
                            }
                        }
                    } else {
                        Enumeration children = node.getParent().children();
                        while (children.hasMoreElements()) {
                            TreeIconNode child = (TreeIconNode) children.nextElement();
                            if (child != node && child.getLabel().equals(value)) {
                                lblError.setText(String.format(bundle.getString("tip_name_exists")));
                                lblError.setVisible(true);
                                return;
                            }
                        }
                    }
                    if (type == TreeCollection.TYPE.PROJECT || type == TreeCollection.TYPE.INTERFACE) {
                        if (type == TreeCollection.TYPE.PROJECT) {
                            mainPanel.history.renameProject(node.getLabel(), value.toString(), collection);
                        }
                    }
                }
                if (entry.type == JTreeTable.EditValueRenderer.TYPE.ADD) {
                    JSONObject json = ((JSONObject) entry.json).getJSONObject(entry.name);
                    if (json.isNullObject()) {
                        json = new JSONObject();
                        ((JSONObject) entry.json).element(entry.name, json);
                    }
                    json.element(txtName.getText(), value);
                    Editor.Entry child = entry.add(json, txtName.getText(), value, EDIT, null);
                    child.isDynamic = true;
                    saveSelectedNode(child);
                } else {
                    if (entry.json instanceof JSONObject) {
                        ((JSONObject) entry.json).element(entry.name, value);
                    } else {
                        ((JSONArray) entry.json).add(value);
                    }
                    entry.value = value;
                    saveSelectedNode(entry);
                }
                dialog.setVisible(false);
            } catch (Exception ex) {
                lblError.setText(String.format(bundle.getString("error_convert"), dataType));
                lblError.setVisible(true);
                logger.log(Level.SEVERE, ex.toString(), ex);
            }
        });
        deleteButton = new JButton(bundle.getString("delete"));
        deleteButton.addActionListener((ActionEvent evt) -> {
            entry.isDelete = true;
            if (entry.json instanceof JSONObject) {
                ((JSONObject) entry.json).remove(entry.name);
            } else {
                ((JSONArray) entry.json).remove(Integer.parseInt(entry.name));
            }
            saveSelectedNode(entry);
            dialog.setVisible(false);
        });
        cancelButton = new JButton(bundle.getString("cancel"));
        cancelButton.addActionListener((ActionEvent evt) -> {
            dialog.setVisible(false);
        });
        okButton = new JButton(bundle.getString("ok"));
        okButton.addActionListener((ActionEvent evt) -> {
            dialog.setVisible(false);
        });
        cmbDataTypeItemStateChanged(null);

        optionPane = new JOptionPane(this);
        dialog = Amphibia.createDialog(optionPane, true);
        dialog.setSize(new Dimension(700, 400));
        java.awt.EventQueue.invokeLater(() -> {
            dialog.setLocationRelativeTo(mainPanel);
        });
    }
    
    private void saveSelectedNode(Editor.Entry entry) {
        mainPanel.history.saveEntry(entry, MainPanel.selectedNode.getCollection());
    }

    @SuppressWarnings("NonPublicExported")
    public void openCreateDialog(Editor.Entry entry) {
        this.entry = entry;
        openEditDialog(entry, null, null, true);
    }

    @SuppressWarnings("NonPublicExported")
    public void openEditDialog(Editor.Entry entry, Object value, boolean isEdit) {
        this.entry = entry;
        openEditDialog(entry, entry.name, value, isEdit);
    }

    @SuppressWarnings("NonPublicExported")
    public void openEditDialog(Editor.Entry entry, String name, Object value, boolean isEdit) {
        if (isEdit) {
            if (entry != null && entry.isDynamic) {
                optionPane.setOptions(new Object[]{applyButton, deleteButton, cancelButton});
            } else {
                optionPane.setOptions(new Object[]{applyButton, cancelButton});
            }
        } else {
            optionPane.setOptions(new Object[]{okButton});
        }
        txtName.setText(name);
        txtName.setEditable(name == null);
        txtName.setBorder(defaultBorder);
        txtEditor.setEditable(isEdit);
        txtEditor.setText(value == null ? "" : String.valueOf(value));
        lblDataType.setVisible(true);
        cmbDataType.setVisible(true);
        cmbDataType.setSelectedItem(getType(value));
        cmbDataType.setEnabled(isEdit && entry.type != EDIT_LIMIT);
        if (value instanceof JSON) {
            try {
                txtEditor.setText(IO.prettyJson(((JSON) value).toString()));
            } catch (Exception ex) {
            }
        } else if (isAny(value)) {
            txtEditor.setText(txtEditor.getText().substring(1, txtEditor.getText().length() - 1));
            lblDataType.setVisible(false);
            cmbDataType.setVisible(false);
        }
        Amphibia.setText(txtEditor, splEditor, null);
        lblError.setVisible(false);
        dialog.setVisible(true);
    }
    
    public static Object getValue(String dataType, String value) throws Exception {
        switch (dataType) {
            case "NULL":
                return null;
            case "Number":
                return NUMBER.parse(value);
            case "Boolean":
                return "true".equals(value);
            case "JSON":
                return IO.prettyJson(value);
            default:
                return value;
        }
    }

    public static String getType(Object value) {
        if (value == null || value == JSONNull.getInstance()) {
            return "NULL";
        }
        if (value instanceof Integer || value instanceof Long || value instanceof Double) {
            return "Number";
        }
        if (value instanceof Boolean) {
            return "Boolean";
        }
        if (value instanceof JSON) {
            return "JSON";
        }
        if (value instanceof String && ((String) value).startsWith("${#") && ((String) value).endsWith("}")) {
            return "Properties";
        }
        return "String";
    }

    public static boolean isAny(Object value) {
        if (value instanceof String) {
            String str = value.toString();
            if (str.startsWith("`") && str.endsWith("`")) {
                return true;
            }
        }
        return false;
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        pnlHeader = new JPanel();
        lblName = new JLabel();
        txtName = new JTextField();
        splEditor = new JScrollPane();
        txtEditor = new JTextArea();
        pnlFooter = new JPanel();
        pnlDataType = new JPanel();
        lblDataType = new JLabel();
        cmbDataType = new JComboBox<>();
        lblError = new JLabel();

        setBorder(BorderFactory.createEmptyBorder(5, 10, 0, 10));
        setLayout(new BorderLayout());

        pnlHeader.setBorder(BorderFactory.createEmptyBorder(1, 1, 5, 1));
        pnlHeader.setLayout(new BoxLayout(pnlHeader, BoxLayout.LINE_AXIS));

        lblName.setFont(new Font("Tahoma", 1, 11)); // NOI18N
        ResourceBundle bundle = ResourceBundle.getBundle("com/equinix/amphibia/messages"); // NOI18N
        lblName.setText(bundle.getString("name")); // NOI18N
        lblName.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 10));
        pnlHeader.add(lblName);
        pnlHeader.add(txtName);

        add(pnlHeader, BorderLayout.PAGE_START);

        txtEditor.setColumns(20);
        txtEditor.setRows(5);
        splEditor.setViewportView(txtEditor);

        add(splEditor, BorderLayout.CENTER);

        pnlFooter.setPreferredSize(new Dimension(603, 60));
        pnlFooter.setLayout(new GridLayout(2, 0));

        lblDataType.setText(bundle.getString("dataType")); // NOI18N
        pnlDataType.add(lblDataType);

        cmbDataType.setModel(new DefaultComboBoxModel<>(new String[] { "NULL", "String", "Boolean", "Number", "Properties", "JSON" }));
        cmbDataType.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent evt) {
                cmbDataTypeItemStateChanged(evt);
            }
        });
        cmbDataType.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                cmbDataTypeActionPerformed(evt);
            }
        });
        pnlDataType.add(cmbDataType);

        pnlFooter.add(pnlDataType);

        lblError.setForeground(new Color(255, 0, 0));
        lblError.setHorizontalAlignment(SwingConstants.CENTER);
        lblError.setText(bundle.getString("error_convert")); // NOI18N
        pnlFooter.add(lblError);

        add(pnlFooter, BorderLayout.PAGE_END);
    }// </editor-fold>//GEN-END:initComponents

    private void cmbDataTypeItemStateChanged(ItemEvent evt) {//GEN-FIRST:event_cmbDataTypeItemStateChanged
        txtEditor.setEnabled(!"NULL".equals(cmbDataType.getSelectedItem()));
        txtEditor.setBackground(UIManager.getColor(txtEditor.isEnabled() ? "TextArea.background" : "TextArea.disabledBackground"));
    }//GEN-LAST:event_cmbDataTypeItemStateChanged

    private void cmbDataTypeActionPerformed(ActionEvent evt) {//GEN-FIRST:event_cmbDataTypeActionPerformed
        if (dialog.isVisible() && "Properties".equals(cmbDataType.getSelectedItem())) {
            java.awt.EventQueue.invokeLater(() -> {
                String s = (String)JOptionPane.showInputDialog(this,
                    bundle.getString("properties_msg"),
                    bundle.getString("properties_title"),
                    JOptionPane.PLAIN_MESSAGE, null,
                    new String[] {"Global", "Project", "TestSuite", "TestCase", "TestStep"},
                    "TestCase");
                if (s != null && !s.isEmpty()) {
                    txtEditor.setText("${#" + s + "#...}");
                }
            });
        }
    }//GEN-LAST:event_cmbDataTypeActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private JComboBox<String> cmbDataType;
    private JLabel lblDataType;
    private JLabel lblError;
    private JLabel lblName;
    private JPanel pnlDataType;
    private JPanel pnlFooter;
    private JPanel pnlHeader;
    private JScrollPane splEditor;
    private JTextArea txtEditor;
    private JTextField txtName;
    // End of variables declaration//GEN-END:variables

}
