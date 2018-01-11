/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.equinix.amphibia.components;

import static com.equinix.amphibia.agent.converter.Converter.RESOURCE_TYPE;

import com.equinix.amphibia.Amphibia;
import com.equinix.amphibia.IO;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.OverlayLayout;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import javax.swing.ListSelectionModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import net.sf.json.JSONObject;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

/**
 *
 * @author dgofman
 */
public final class ProjectDialog extends javax.swing.JPanel {

    private static final Logger logger = Logger.getLogger(ProjectDialog.class.getName());

    private JDialog dialog;
    private MainPanel mainPanel;
    private final ResourceBundle bundle;
    private TreeCollection selectedProject;
    private DefaultTableModel resourceModel;
    private Map<Integer, Boolean> swaggerDocType;
    private int dataModelIndex;
    private boolean isAddResource;

    private final String SET_URL = "SET_URL";
    private final String SET_FILE = "SET_FILE";
    private final Border DEFAULT_BORDER;
    private final Border ERROR_BORDER = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.RED),
            BorderFactory.createEmptyBorder(2, 2, 2, 2));

    /**
     * Creates new form ProjectDialog
     *
     * @param mainPanel
     */
    public ProjectDialog(MainPanel mainPanel) {
        this.mainPanel = mainPanel;

        bundle = Amphibia.getBundle();

        resourceModel = new DefaultTableModel(
                new Object[][]{},
                new String[]{
                    bundle.getString("swaggerDocument"),
                    bundle.getString("interfaceName"),
                    bundle.getString("rulesProperties")
                }
        ) {
            @Override
            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return columnIndex == 1;
            }
        };

        initComponents();
        txtProjectName.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                selectedProject.setProjectName(txtProjectName.getText());
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
            }
        });

        DEFAULT_BORDER = txtSwaggerUrl.getBorder();

        Font font = tblResources.getFont();
        tblResources.getTableHeader().setFont(font.deriveFont(Font.BOLD));
        tblResources.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        final TableColumnModel colModel = tblResources.getColumnModel();
        for (int c = 0; c < colModel.getColumnCount(); c++) {
            colModel.getColumn(c).setCellRenderer(new DefaultTableCellRenderer() {
                @Override
                public Component getTableCellRendererComponent(JTable table,
                        Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                    super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                    setToolTipText((String) table.getValueAt(row, column));
                    return this;
                }
            });
        }

        colModel.getColumn(0).setPreferredWidth(400);
        colModel.getColumn(1).setPreferredWidth(150);
        tblResources.setAutoResizeMode(JTable.AUTO_RESIZE_NEXT_COLUMN);
        colModel.getColumn(2).setPreferredWidth(400);

        dialog = Amphibia.createDialog(this, new Object[]{}, false);
        dialog.setMinimumSize(new Dimension(getWidth(), getHeight()));
        java.awt.EventQueue.invokeLater(() -> {
            dialog.setLocationRelativeTo(mainPanel);
        });

        lpnLayer.add(pnlWaitOverlay, 0, 0);
        lblAnimation.setLocation((getWidth() - lblAnimation.getWidth()) / 2, (getHeight() - lblAnimation.getHeight()) / 2);
        pnlWaitOverlay.setVisible(false);
    }

    public void openDialog(TreeCollection selectedProject) {
        this.selectedProject = selectedProject;
        pnlSetup.setVisible(true);
        pnlNext.setVisible(false);
        dataModelIndex = 0;
        swaggerDocType = new HashMap<>();
        resourceModel.setRowCount(0);
        for (int i = 0; i < selectedProject.swaggers.getChildCount(); i++) {
            TreeIconNode swagger = (TreeIconNode) selectedProject.swaggers.getChildAt(i);
            TreeIconNode intrf = (TreeIconNode) swagger.getFirstChild();
            String rules = "";
            if (swagger.getChildCount() > 1) {
                rules = ((TreeIconNode) swagger.getChildAt(1)).getTreeIconUserObject().getFullPath();
            }
            resourceModel.addRow(new String[]{
                swagger.getTreeIconUserObject().getFullPath(),
                intrf.getTreeIconUserObject().getLabel(),
                rules});
            swaggerDocType.put(i, swagger.getTreeIconUserObject().isURL());
        }
        txtProjectName.setText(selectedProject.getProjectName());
        if (selectedProject.getProjectFile() != null) {
            txtLocation.setText(selectedProject.getProjectFile().getAbsolutePath());
        } else {
            txtLocation.setText("");
        }
        resetSetup();
        dialog.setVisible(true);
    }

    private void resetSetup() {
        txtSwaggerUrl.setText("");
        txtSwaggerFile.setText("");
        txtRulesFile.setText("");
        invalidateAll();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        rbgSwagger = new ButtonGroup();
        pnlWaitOverlay = new JPanel();
        lblAnimation = new JLabel();
        lpnLayer = new JLayeredPane();
        pnlSetup = new JPanel();
        lblSwagger = new JLabel();
        spr1 = new JSeparator();
        rbnSwaggerUrl = new JRadioButton();
        txtSwaggerUrl = new JTextField();
        lblSwaggerUrlError = new JLabel();
        btnSwaggerUrl = new JButton();
        rbnSwaggerFile = new JRadioButton();
        txtSwaggerFile = new JTextField();
        lblSwaggerFileError = new JLabel();
        btnSwaggerFile = new JButton();
        lblRulesAndProperties = new JLabel();
        spr2 = new JSeparator();
        lblRulesFile = new JLabel();
        txtRulesFile = new JTextField();
        lblRulesError = new JLabel();
        btnRulesFile = new JButton();
        spr3 = new JSeparator();
        btnNext = new JButton();
        btnFinish = new JButton();
        btnCancel = new JButton();
        btnHelp = new JButton();
        pnlNext = new JPanel();
        spr4 = new JSeparator();
        lblProject = new JLabel();
        lblName = new JLabel();
        lblNameError = new JLabel();
        txtProjectName = new JTextField();
        lblLocation = new JLabel();
        txtLocation = new JTextField();
        btnLocation = new JButton();
        lblLocationError = new JLabel();
        lblResources = new JLabel();
        btnAddResources = new JButton();
        slpResources = new JScrollPane();
        tblResources = new JTable();
        spr5 = new JSeparator();
        btnBack = new JButton();
        btnNextFinish = new JButton();
        btnNextCancel = new JButton();

        pnlWaitOverlay.setBackground(Amphibia.OVERLAY_BG_COLOR);
        pnlWaitOverlay.setLayout(null);

        lblAnimation.setHorizontalAlignment(SwingConstants.CENTER);
        lblAnimation.setIcon(new ImageIcon(getClass().getResource("/com/equinix/amphibia/icons/ajax-loader.gif"))); // NOI18N
        lblAnimation.setOpaque(true);
        pnlWaitOverlay.add(lblAnimation);
        lblAnimation.setBounds(0, 0, 60, 42);

        setPreferredSize(new Dimension(775, 290));
        setLayout(new OverlayLayout(this));

        lpnLayer.setLayout(new OverlayLayout(lpnLayer));

        pnlSetup.setLayout(null);

        lblSwagger.setFont(new Font("Tahoma", 1, 11)); // NOI18N
        ResourceBundle bundle = ResourceBundle.getBundle("com/equinix/amphibia/messages"); // NOI18N
        lblSwagger.setText(bundle.getString("swaggerDocument")); // NOI18N
        pnlSetup.add(lblSwagger);
        lblSwagger.setBounds(10, 10, 650, 14);
        pnlSetup.add(spr1);
        spr1.setBounds(10, 30, 750, 10);

        rbgSwagger.add(rbnSwaggerUrl);
        rbnSwaggerUrl.setSelected(true);
        rbnSwaggerUrl.setText(bundle.getString("url")); // NOI18N
        rbnSwaggerUrl.setActionCommand(SET_URL);
        rbnSwaggerUrl.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                rbnSwaggerUrlActionPerformed(evt);
            }
        });
        pnlSetup.add(rbnSwaggerUrl);
        rbnSwaggerUrl.setBounds(10, 50, 70, 23);
        pnlSetup.add(txtSwaggerUrl);
        txtSwaggerUrl.setBounds(90, 50, 570, 21);

        lblSwaggerUrlError.setForeground(new Color(255, 51, 51));
        lblSwaggerUrlError.setText(bundle.getString("error_open_json")); // NOI18N
        pnlSetup.add(lblSwaggerUrlError);
        lblSwaggerUrlError.setBounds(90, 70, 570, 14);

        btnSwaggerUrl.setText(bundle.getString("load")); // NOI18N
        btnSwaggerUrl.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                btnSwaggerUrlActionPerformed(evt);
            }
        });
        pnlSetup.add(btnSwaggerUrl);
        btnSwaggerUrl.setBounds(670, 50, 90, 23);

        rbgSwagger.add(rbnSwaggerFile);
        rbnSwaggerFile.setText(bundle.getString("file")); // NOI18N
        rbnSwaggerFile.setActionCommand(SET_FILE);
        rbnSwaggerFile.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                rbnSwaggerFileActionPerformed(evt);
            }
        });
        pnlSetup.add(rbnSwaggerFile);
        rbnSwaggerFile.setBounds(10, 90, 70, 23);

        txtSwaggerFile.setEditable(false);
        pnlSetup.add(txtSwaggerFile);
        txtSwaggerFile.setBounds(90, 90, 570, 21);

        lblSwaggerFileError.setForeground(new Color(255, 51, 51));
        lblSwaggerFileError.setText(bundle.getString("error_open_json")); // NOI18N
        pnlSetup.add(lblSwaggerFileError);
        lblSwaggerFileError.setBounds(90, 110, 570, 14);

        btnSwaggerFile.setText(bundle.getString("browse")); // NOI18N
        btnSwaggerFile.setEnabled(false);
        btnSwaggerFile.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                btnSwaggerFileActionPerformed(evt);
            }
        });
        pnlSetup.add(btnSwaggerFile);
        btnSwaggerFile.setBounds(670, 90, 90, 23);

        lblRulesAndProperties.setFont(new Font("Tahoma", 1, 11)); // NOI18N
        lblRulesAndProperties.setText(bundle.getString("rulesProperties")); // NOI18N
        pnlSetup.add(lblRulesAndProperties);
        lblRulesAndProperties.setBounds(10, 140, 640, 14);
        pnlSetup.add(spr2);
        spr2.setBounds(10, 160, 750, 10);

        lblRulesFile.setText(bundle.getString("file")); // NOI18N
        pnlSetup.add(lblRulesFile);
        lblRulesFile.setBounds(30, 180, 50, 20);

        txtRulesFile.setEditable(false);
        pnlSetup.add(txtRulesFile);
        txtRulesFile.setBounds(90, 180, 570, 21);

        lblRulesError.setForeground(new Color(255, 51, 51));
        lblRulesError.setText(bundle.getString("error_open_json")); // NOI18N
        pnlSetup.add(lblRulesError);
        lblRulesError.setBounds(90, 200, 570, 14);

        btnRulesFile.setText(bundle.getString("browse")); // NOI18N
        btnRulesFile.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                btnRulesFileActionPerformed(evt);
            }
        });
        pnlSetup.add(btnRulesFile);
        btnRulesFile.setBounds(670, 180, 90, 23);
        pnlSetup.add(spr3);
        spr3.setBounds(0, 250, 780, 10);

        btnNext.setText(bundle.getString("next")); // NOI18N
        btnNext.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                btnNextActionPerformed(evt);
            }
        });
        pnlSetup.add(btnNext);
        btnNext.setBounds(370, 260, 90, 23);

        btnFinish.setText(bundle.getString("finish")); // NOI18N
        btnFinish.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                btnFinishActionPerformed(evt);
            }
        });
        pnlSetup.add(btnFinish);
        btnFinish.setBounds(470, 260, 90, 23);

        btnCancel.setText(bundle.getString("cancel")); // NOI18N
        btnCancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                btnCancelActionPerformed(evt);
            }
        });
        pnlSetup.add(btnCancel);
        btnCancel.setBounds(570, 260, 90, 23);

        btnHelp.setText(bundle.getString("mnuHelp")); // NOI18N
        btnHelp.setEnabled(false);
        pnlSetup.add(btnHelp);
        btnHelp.setBounds(670, 260, 90, 23);

        lpnLayer.add(pnlSetup);

        pnlNext.setMinimumSize(new Dimension(775, 290));
        pnlNext.setLayout(null);
        pnlNext.add(spr4);
        spr4.setBounds(0, 250, 780, 10);

        lblProject.setFont(new Font("Tahoma", 1, 11)); // NOI18N
        lblProject.setText(bundle.getString("project")); // NOI18N
        pnlNext.add(lblProject);
        lblProject.setBounds(10, 0, 650, 14);

        lblName.setText(bundle.getString("name")); // NOI18N
        pnlNext.add(lblName);
        lblName.setBounds(12, 33, 80, 14);

        lblNameError.setForeground(new Color(255, 51, 51));
        lblNameError.setText(bundle.getString("error_project_exists")); // NOI18N
        pnlNext.add(lblNameError);
        lblNameError.setBounds(90, 50, 670, 14);
        pnlNext.add(txtProjectName);
        txtProjectName.setBounds(90, 30, 670, 21);

        lblLocation.setText(bundle.getString("location")); // NOI18N
        pnlNext.add(lblLocation);
        lblLocation.setBounds(10, 73, 80, 14);

        txtLocation.setEditable(false);
        pnlNext.add(txtLocation);
        txtLocation.setBounds(90, 70, 570, 21);

        btnLocation.setText(bundle.getString("browse")); // NOI18N
        btnLocation.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                btnLocationActionPerformed(evt);
            }
        });
        pnlNext.add(btnLocation);
        btnLocation.setBounds(670, 70, 90, 23);

        lblLocationError.setForeground(new Color(255, 51, 51));
        lblLocationError.setText(bundle.getString("error_open_json")); // NOI18N
        pnlNext.add(lblLocationError);
        lblLocationError.setBounds(90, 90, 670, 14);

        lblResources.setFont(new Font("Tahoma", 1, 11)); // NOI18N
        lblResources.setText(bundle.getString("swaggers")); // NOI18N
        pnlNext.add(lblResources);
        lblResources.setBounds(10, 110, 650, 14);

        btnAddResources.setText(bundle.getString("addResources")); // NOI18N
        btnAddResources.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                btnAddResourcesActionPerformed(evt);
            }
        });
        pnlNext.add(btnAddResources);
        btnAddResources.setBounds(10, 260, 130, 23);

        tblResources.setModel(resourceModel);
        tblResources.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        tblResources.setShowHorizontalLines(false);
        slpResources.setViewportView(tblResources);

        pnlNext.add(slpResources);
        slpResources.setBounds(10, 130, 750, 110);
        pnlNext.add(spr5);
        spr5.setBounds(10, 20, 750, 10);

        btnBack.setText(bundle.getString("back")); // NOI18N
        btnBack.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                btnBackActionPerformed(evt);
            }
        });
        pnlNext.add(btnBack);
        btnBack.setBounds(470, 260, 90, 23);

        btnNextFinish.setText(bundle.getString("finish")); // NOI18N
        btnNextFinish.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                btnFinishActionPerformed(evt);
            }
        });
        pnlNext.add(btnNextFinish);
        btnNextFinish.setBounds(570, 260, 90, 23);

        btnNextCancel.setText(bundle.getString("cancel")); // NOI18N
        btnNextCancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                btnCancelActionPerformed(evt);
            }
        });
        pnlNext.add(btnNextCancel);
        btnNextCancel.setBounds(670, 260, 90, 23);

        lpnLayer.add(pnlNext);

        add(lpnLayer);
    }// </editor-fold>//GEN-END:initComponents

    private void rbnSwaggerUrlActionPerformed(ActionEvent evt) {//GEN-FIRST:event_rbnSwaggerUrlActionPerformed
        invalidateAll();
    }//GEN-LAST:event_rbnSwaggerUrlActionPerformed

    private void btnSwaggerUrlActionPerformed(ActionEvent evt) {//GEN-FIRST:event_btnSwaggerUrlActionPerformed
        txtSwaggerUrl.setBorder(DEFAULT_BORDER);
        final String url = txtSwaggerUrl.getText();
        if (url == null || !url.startsWith("http")) {
            lblSwaggerUrlError.setText(bundle.getString("tip_invalid_url"));
            txtSwaggerUrl.setBorder(ERROR_BORDER);
        } else if (isExists(url)) {
            lblSwaggerUrlError.setText(bundle.getString("tip_file_exists"));
            txtSwaggerUrl.setBorder(ERROR_BORDER);
        } else {
            pnlWaitOverlay.setVisible(true);
            new Thread() {
                @Override
                public void run() {
                    try {
                        loadProject(url, new URL(url).openStream(), true);
                        txtSwaggerUrl.setText(url);
                        invalidateAll();
                    } catch (Exception e) {
                        lblSwaggerUrlError.setText(bundle.getString("error_open_json"));
                    }
                    pnlWaitOverlay.setVisible(false);
                }
            }.start();
        }
    }//GEN-LAST:event_btnSwaggerUrlActionPerformed

    private void btnSwaggerFileActionPerformed(ActionEvent evt) {//GEN-FIRST:event_btnSwaggerFileActionPerformed
        JFileChooser jf = Amphibia.setFileChooserDir(new JFileChooser());
        jf.setFileFilter(new FileNameExtensionFilter("Swagger JSON File", "json", "text"));
        jf.showOpenDialog(null);
        if (jf.getSelectedFile() != null) {
            String file = jf.getSelectedFile().getAbsolutePath();
            if (isExists(file)) {
                txtSwaggerFile.setText(file);
                lblSwaggerFileError.setText(bundle.getString("tip_file_exists"));
                txtSwaggerFile.setBorder(ERROR_BORDER);
            } else {
                Amphibia.saveFileChooserDir(jf);
                pnlWaitOverlay.setVisible(true);
                new Thread() {
                    @Override
                    public void run() {
                        try {
                            FileInputStream fis = new FileInputStream(jf.getSelectedFile());
                            loadProject(file, fis, false);
                            fis.close();
                            txtSwaggerFile.setText(file);
                            invalidateAll();
                        } catch (Exception e) {
                            lblSwaggerFileError.setText(bundle.getString("error_open_json"));
                        }
                        pnlWaitOverlay.setVisible(false);
                    }
                }.start();
            }
        } else {
            txtSwaggerFile.setText("");
            if (resourceModel.getRowCount() > dataModelIndex) {
                resourceModel.setValueAt(null, dataModelIndex, 0);
                resourceModel.setValueAt(null, dataModelIndex, 1);
            }
            invalidateAll();
        }
    }//GEN-LAST:event_btnSwaggerFileActionPerformed

    private void btnRulesFileActionPerformed(ActionEvent evt) {//GEN-FIRST:event_btnRulesFileActionPerformed
        JFileChooser jf = Amphibia.setFileChooserDir(new JFileChooser());
        jf.setFileFilter(new FileNameExtensionFilter("Properties JSON File", "json", "text"));
        jf.showOpenDialog(null);
        if (jf.getSelectedFile() != null) {
            Amphibia.saveFileChooserDir(jf);
            pnlWaitOverlay.setVisible(true);
            new Thread() {
                @Override
                public void run() {
                    try {
                        JSONObject json = (JSONObject) IO.getJSON(jf.getSelectedFile());
                        for (Object[] prop : TreeCollection.RULES_PROPERTIES) {
                            String name = prop[0].toString();
                            if (!json.containsKey(name)) {
                                throw new Exception(String.format(bundle.getString("error_rules_format"), name));
                            }
                        }
                        txtRulesFile.setText(jf.getSelectedFile().getAbsolutePath());
                        if (resourceModel.getRowCount() <= dataModelIndex) {
                            resourceModel.addRow(new String[]{"", "", ""});
                        }
                        resourceModel.setValueAt(txtRulesFile.getText(), dataModelIndex, 2);
                    } catch (Exception e) {
                        lblRulesError.setText(bundle.getString("error_open_json"));
                        logger.log(Level.SEVERE, e.getMessage(), e);
                    }
                    pnlWaitOverlay.setVisible(false);
                }
            }.start();
        } else {
            txtRulesFile.setText("");
            if (resourceModel.getRowCount() > dataModelIndex) {
                resourceModel.setValueAt(null, dataModelIndex, 2);
            }
            invalidateAll();
        }
    }//GEN-LAST:event_btnRulesFileActionPerformed

    private void btnNextActionPerformed(ActionEvent evt) {//GEN-FIRST:event_btnNextActionPerformed
        boolean isURL = SET_URL.equals(rbgSwagger.getSelection().getActionCommand());
        if (isURL) {
            String fileOrUrl = null;
            if (resourceModel.getRowCount() > dataModelIndex) {
                fileOrUrl = (String) resourceModel.getValueAt(dataModelIndex, 0);
            }
            if (fileOrUrl == null || !fileOrUrl.equals(txtSwaggerUrl.getText())) {
                btnSwaggerUrlActionPerformed(evt);
            }
        }

        isAddResource = false;
        pnlSetup.setVisible(false);
        pnlNext.setVisible(true);
    }

    public void createProject() {
        int count = resourceModel.getRowCount();
        String[] inputs = new String[count];
        String[] properties = new String[count];
        String[] interfaces = new String[count];
        for (int index = 0; index < count; index++) {
            Object rules = resourceModel.getValueAt(index, 2);
            inputs[index] = resourceModel.getValueAt(index, 0).toString();
            interfaces[index] = resourceModel.getValueAt(index, 1).toString();
            properties[index] = rules != null ? rules.toString() : "";
        }
        try {
            String[] args = new String[]{
                "-n=" + selectedProject.getProjectName(),
                "-a=" + selectedProject.getProjectFile(),
                "-i=" + String.join(",", inputs),
                "-p=" + String.join(",", properties),
                "-f=" + String.join(",", interfaces),
                "-j=true", "-d=true"
            };
            logger.log(Level.INFO, String.join(" ", args));
            Map<RESOURCE_TYPE, Object> results = com.equinix.amphibia.agent.converter.Converter.execute(args);
            logger.info(results.toString());
            List<String> items = (List<String>) results.get(RESOURCE_TYPE.warnings);
            items.forEach(mainPanel.editor::addWarning);
            items = (List<String>) results.get(RESOURCE_TYPE.errors);
            items.forEach(mainPanel.editor::addError);
        } catch (Exception e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
        }
    }//GEN-LAST:event_btnNextActionPerformed

    private void rbnSwaggerFileActionPerformed(ActionEvent evt) {//GEN-FIRST:event_rbnSwaggerFileActionPerformed
        invalidateAll();
    }//GEN-LAST:event_rbnSwaggerFileActionPerformed

    private void btnCancelActionPerformed(ActionEvent evt) {//GEN-FIRST:event_btnCancelActionPerformed
        if (isAddResource) {
            btnNextActionPerformed(evt);
        } else {
            dialog.setVisible(false);
        }
    }//GEN-LAST:event_btnCancelActionPerformed

    private void btnFinishActionPerformed(ActionEvent evt) {//GEN-FIRST:event_btnFinishActionPerformed
        try {
            if (txtProjectName.getText().isEmpty()) {
                dialog.setVisible(false);
                return;
            }
            
            Enumeration children = mainPanel.treeNode.children();
            while (children.hasMoreElements()) {
                if (txtProjectName.getText().equals(((TreeIconNode)children.nextElement()).getLabel())) {
                    lblNameError.setText(bundle.getString("error_project_exists"));
                    pnlSetup.setVisible(false);
                    pnlNext.setVisible(true);
                    return;
                }
            }

            selectedProject.setProjectName(txtProjectName.getText().trim(), mainPanel.treeNav);

            for (int r = 0; r < resourceModel.getRowCount(); r++) {
                Object swagger = resourceModel.getValueAt(r, 0);
                Object rules = resourceModel.getValueAt(r, 2);
                if (swagger == null || swagger.toString().isEmpty()) {
                    if (rules == null || rules.toString().isEmpty()) {
                        continue; //skip empty rows
                    }
                    tblResources.setRowSelectionInterval(0, r);
                    btnBackActionPerformed(evt);
                    if (rbnSwaggerUrl.isSelected()) {
                        lblSwaggerUrlError.setText(bundle.getString("error_missing_swagger"));
                    } else {
                        lblSwaggerFileError.setText(bundle.getString("error_missing_swagger"));
                    }
                    return;
                }
            }

            if (!selectedProject.getProjectDir().exists()) {
                selectedProject.getProjectDir().mkdirs();
            }

            if (selectedProject.getProjectDir().list().length > 0) {
                int value = JOptionPane.showConfirmDialog(this,
                        bundle.getString("tip_delete_files") + "\n\n",
                        bundle.getString("title"),
                        JOptionPane.YES_NO_OPTION);
                if (value == JOptionPane.NO_OPTION) {
                    return;
                }
            }

            FileUtils.cleanDirectory(selectedProject.getProjectDir());

            pnlWaitOverlay.setVisible(true);
            new Thread() {
                @Override
                public void run() {
                    if (resourceModel.getRowCount() > 0) {
                        createProject();
                    }
                    pnlWaitOverlay.setVisible(false);
                    mainPanel.editor.getTabs().setSelectedIndex(0);
                    mainPanel.loadProject(selectedProject);
                    mainPanel.expandDefaultNodes(selectedProject);
                    selectedProject.save();
                    dialog.setVisible(false);
                }
            }.start();
        } catch (IOException ex) {
            logger.log(Level.SEVERE, ex.toString(), ex);
            JOptionPane.showMessageDialog(this,
                    ex.getMessage(), bundle.getString("title"), JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_btnFinishActionPerformed

    private void btnLocationActionPerformed(ActionEvent evt) {//GEN-FIRST:event_btnLocationActionPerformed
        JFileChooser jf = Amphibia.setFileChooserDir(new JFileChooser());
        jf.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        jf.showOpenDialog(null);
        if (jf.getSelectedFile() != null) {
            pnlWaitOverlay.setVisible(true);
            new Thread() {
                @Override
                public void run() {
                    try {
                        if (jf.getSelectedFile().isDirectory()) {
                            if (selectedProject.getProjectName().isEmpty()) {
                                txtProjectName.setText("Project");
                            }
                            selectedProject.setProjectFile(new File(jf.getSelectedFile(), selectedProject.getProjectName() + ".json"));
                        } else {
                            if (selectedProject.getProjectFile() != null && selectedProject.getProjectFile().exists()) {
                                selectedProject.getProjectFile().renameTo(jf.getSelectedFile());
                            }
                            selectedProject.setProjectFile(jf.getSelectedFile());
                        }
                        txtLocation.setText(selectedProject.getProjectFile().getCanonicalPath());
                        lblLocationError.setText("");
                    } catch (IOException ex) {
                        logger.log(Level.SEVERE, ex.toString(), ex);
                        lblLocationError.setText(ex.getMessage());
                    }
                    pnlWaitOverlay.setVisible(false);
                }
            }.start();
        }
    }//GEN-LAST:event_btnLocationActionPerformed

    private void btnAddResourcesActionPerformed(ActionEvent evt) {//GEN-FIRST:event_btnAddResourcesActionPerformed
        isAddResource = true;
        tblResources.clearSelection();
        dataModelIndex = resourceModel.getRowCount();
        resetSetup();
        pnlNext.setVisible(false);
        pnlSetup.setVisible(true);
    }//GEN-LAST:event_btnAddResourcesActionPerformed

    private void btnBackActionPerformed(ActionEvent evt) {//GEN-FIRST:event_btnBackActionPerformed
        if (tblResources.getSelectedRow() != -1) {
            dataModelIndex = tblResources.getSelectedRow();
            if (resourceModel.getRowCount() > dataModelIndex) {
                txtSwaggerUrl.setText("");
                txtSwaggerFile.setText("");
                String value = resourceModel.getValueAt(dataModelIndex, 0).toString();
                if (swaggerDocType.containsKey(dataModelIndex) && swaggerDocType.get(dataModelIndex)) {
                    txtSwaggerUrl.setText(value);
                    rbnSwaggerUrl.setSelected(true);
                } else {
                    txtSwaggerFile.setText(value);
                    rbnSwaggerFile.setSelected(true);
                }
                txtRulesFile.setText(resourceModel.getValueAt(dataModelIndex, 2).toString());
                invalidateAll();
            }
        }
        pnlNext.setVisible(false);
        pnlSetup.setVisible(true);
    }//GEN-LAST:event_btnBackActionPerformed

    private void loadProject(String fileOrUrl, InputStream is, Boolean isURL) throws Exception {
        txtSwaggerUrl.setText("");
        txtSwaggerFile.setText("");
        try {
            JSONObject json = JSONObject.fromObject(IOUtils.toString(is));
            logger.log(Level.INFO, json.toString());
            if (!json.containsKey("swagger") || !json.containsKey("paths")) {
                throw new Exception(bundle.getString("error_swagger_format"));
            }

            String projectName = selectedProject.getProjectName();
            if (projectName.isEmpty() || selectedProject.getProjectFile() == null) {
                JSONObject info = json.getJSONObject("info");
                if (info != null && info.containsKey("title")) {
                    projectName = info.getString("title").replaceAll("\\s", "");
                }

                if (projectName == null || projectName.isEmpty()) {
                    if (isURL) {
                        projectName = "Project";
                    } else {
                        File file = new File(fileOrUrl);
                        if (file.getName().lastIndexOf(".") != -1) {
                            projectName = file.getName().substring(0, file.getName().lastIndexOf("."));
                        } else {
                            projectName = file.getName();
                        }
                    }
                }
                selectedProject.setProjectName(projectName);
                selectedProject.setProjectFile(IO.getFile(selectedProject, projectName + ".json"));
            }

            txtLocation.setText(selectedProject.getProjectFile().getCanonicalPath());
            txtProjectName.setText(selectedProject.getProjectName());

            if (resourceModel.getRowCount() <= dataModelIndex) {
                resourceModel.addRow(new String[]{"", "", ""});
            }
            resourceModel.setValueAt(fileOrUrl, dataModelIndex, 0);
            resourceModel.setValueAt(json.getString("basePath"), dataModelIndex, 1);
            swaggerDocType.put(dataModelIndex, isURL);
            tblResources.setRowSelectionInterval(0, dataModelIndex);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Parse: " + fileOrUrl, e);
            throw e;
        }
    }

    public void invalidateAll() {
        txtSwaggerUrl.setBorder(DEFAULT_BORDER);
        txtSwaggerFile.setBorder(DEFAULT_BORDER);
        lblSwaggerUrlError.setText("");
        lblSwaggerFileError.setText("");
        lblNameError.setText("");
        lblLocationError.setText("");
        lblRulesError.setText("");

        boolean isURL = SET_URL.equals(rbgSwagger.getSelection().getActionCommand());
        txtSwaggerUrl.setEnabled(isURL);
        btnSwaggerUrl.setEnabled(isURL);
        txtSwaggerFile.setEnabled(!isURL);
        btnSwaggerFile.setEnabled(!isURL);
    }

    private boolean isExists(String filePath) {
        for (int r = 0; r < resourceModel.getRowCount(); r++) {
            if (resourceModel.getValueAt(r, 0).toString().equals(filePath)) {
                return true;
            }
        }
        return false;
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private JButton btnAddResources;
    private JButton btnBack;
    private JButton btnCancel;
    private JButton btnFinish;
    private JButton btnHelp;
    private JButton btnLocation;
    private JButton btnNext;
    private JButton btnNextCancel;
    private JButton btnNextFinish;
    private JButton btnRulesFile;
    private JButton btnSwaggerFile;
    private JButton btnSwaggerUrl;
    private JLabel lblAnimation;
    private JLabel lblLocation;
    private JLabel lblLocationError;
    private JLabel lblName;
    private JLabel lblNameError;
    private JLabel lblProject;
    private JLabel lblResources;
    private JLabel lblRulesAndProperties;
    private JLabel lblRulesError;
    private JLabel lblRulesFile;
    private JLabel lblSwagger;
    private JLabel lblSwaggerFileError;
    private JLabel lblSwaggerUrlError;
    private JLayeredPane lpnLayer;
    private JPanel pnlNext;
    private JPanel pnlSetup;
    private JPanel pnlWaitOverlay;
    private ButtonGroup rbgSwagger;
    private JRadioButton rbnSwaggerFile;
    private JRadioButton rbnSwaggerUrl;
    private JScrollPane slpResources;
    private JSeparator spr1;
    private JSeparator spr2;
    private JSeparator spr3;
    private JSeparator spr4;
    private JSeparator spr5;
    private JTable tblResources;
    private JTextField txtLocation;
    private JTextField txtProjectName;
    private JTextField txtRulesFile;
    private JTextField txtSwaggerFile;
    private JTextField txtSwaggerUrl;
    // End of variables declaration//GEN-END:variables
}
