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
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Arrays;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javax.swing.border.Border;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;

/**
 *
 * @author dgofman
 */
public class GlobalVariableDialog extends javax.swing.JPanel {

    private final MainPanel mainPanel;
    private final JDialog dialog;
    private final ResourceBundle bundle;
    private final DefaultTableModel globalVarsModel;
    
    private final int firstColumnWidth = 20;
    private final int secondColumnWidth = 200;
    private final int defaultColumnWidth = 70;
    private final int borderGap = 50;
    private final int defaultWidth = 700;
   
    private static GlobalVarModel globalVarsSource;

    private static final NumberFormat NUMBER = NumberFormat.getInstance();
    private static final Preferences userPreferences = Amphibia.getUserPreferences();
    private static final Logger logger = Logger.getLogger(GlobalVariableDialog.class.getName());

    /**
     * Creates new form GlobalVaraibleDialog
     */
    public GlobalVariableDialog(MainPanel mainPanel) {
        this.mainPanel = mainPanel;

        bundle = Amphibia.getBundle();
        
        byte[] data = userPreferences.getByteArray(Amphibia.P_GLOBAL_VARS, null);
        if (data == null) {
            globalVarsSource = new GlobalVarModel();
        } else {
            try {
                globalVarsSource = (GlobalVarModel) new ObjectInputStream(new ByteArrayInputStream(data)).readObject();
            } catch (IOException | ClassNotFoundException ex) {
                globalVarsSource = new GlobalVarModel();
                logger.log(Level.SEVERE, null, ex);
            }
        }
        globalVarsSource.columns[1] = bundle.getString("name");
        globalVarsSource.columns[2] = bundle.getString("default");

        globalVarsModel = new DefaultTableModel(
                globalVarsSource.data, globalVarsSource.columns) {
            @Override
            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return columnIndex > 1;
            }
        };

        initComponents();

        final ImageIcon closeIcon = new ImageIcon(Amphibia.class.getResource("/com/equinix/amphibia/icons/close_16.png"));
        final JLabel closeLabel = new JLabel();
        closeLabel.setIcon(closeIcon);
        
        Font font = tblVars.getFont();
        JTableHeader header = tblVars.getTableHeader();
        header.setFont(font.deriveFont(Font.BOLD));
        tblVars.getColumnModel().getColumn(0).setMinWidth(16);
        tblVars.getColumnModel().getColumn(0).setMaxWidth(16);

        tblVars.setAutoCreateColumnsFromModel(false);
        tblVars.getColumnModel().getColumn(1).setPreferredWidth(secondColumnWidth);
        tblVars.getColumnModel().getColumn(2).setPreferredWidth(defaultWidth - firstColumnWidth - secondColumnWidth - 
                borderGap - ((globalVarsModel.getColumnCount() - 3) * defaultColumnWidth));
        tblVars.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        Border border = BorderFactory.createEmptyBorder(4, 5, 2, 5);
        TableCellRenderer renderer = tblVars.getDefaultRenderer(Object.class);
        tblVars.setDefaultRenderer(Object.class, (JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) -> {
            JLabel label = (JLabel) renderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (value != null) {
                label.setToolTipText(value.toString());
            }
            if (column == 0) {
                return closeLabel;
            }
            if (column == 1) {
                int type = (int) globalVarsModel.getValueAt(row, 0);
                label.setIcon(type == 0 ? btnAddEndPoint.getIcon() : btnAddVar.getIcon());
            } else {
                label.setIcon(null);
            }
            label.setBorder(border);
            return label;
        });
        tblVars.setRowHeight(20);
        
        for (int c = 0; c < tblVars.getColumnCount(); c++) {
            TableCellRenderer headerRenderer = header.getDefaultRenderer();
            tblVars.getColumnModel().getColumn(c).setHeaderRenderer((JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) -> {
                JLabel label = (JLabel) headerRenderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                label.setToolTipText(value.toString());
                if (column > 2) {
                    label.setIcon(closeIcon);
                }
                return label;
            });
        }
        
        tblVars.addMouseListener(new MouseAdapter() {
         @Override
         public void mouseReleased(MouseEvent me) {
             int columnIndex = tblVars.columnAtPoint(me.getPoint());
             if (columnIndex == 0) {
                 int rowIndex = tblVars.rowAtPoint(me.getPoint());
                 int n = JOptionPane.showConfirmDialog(dialog,
                         String.format(bundle.getString("tip_delete_env_var"), globalVarsModel.getValueAt(rowIndex, 1)), bundle.getString("title"),
                         JOptionPane.YES_NO_OPTION);
                 if (n == JOptionPane.YES_OPTION) {
                     globalVarsModel.removeRow(rowIndex);
                 }
             }
         }
      });
        

        header.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent evt) {
                JTable table = ((JTableHeader) evt.getSource()).getTable();
                TableColumnModel colModel = table.getColumnModel();

                int index = colModel.getColumnIndexAtX(evt.getX());
                if (index < 3) {
                    return;
                }
                Rectangle headerRect = header.getHeaderRect(index);
                if (headerRect.contains(evt.getX(), evt.getY()) && evt.getX() <= (headerRect.x + closeIcon.getIconWidth())) {
                    int n = JOptionPane.showConfirmDialog(dialog,
                        String.format(bundle.getString("tip_delete_env"), globalVarsSource.columns[index]), bundle.getString("title"),
                        JOptionPane.YES_NO_OPTION);
                    if (n == JOptionPane.YES_OPTION) {
                        System.out.println("DELETE");
                    }
                }
            }
        });

        dialog = Amphibia.createDialog(this, new Object[]{}, true);

        dialog.setSize(new Dimension(defaultWidth, 450));
        dialog.setMinimumSize(new Dimension(650, 300));
        java.awt.EventQueue.invokeLater(() -> {
                dialog.setLocationRelativeTo(mainPanel);
            }
        );
    }

    public void openDialog() {
        dialog.setVisible(true);
    }

    public static String[] getEnvironmentNames() {
        return Arrays.copyOfRange(globalVarsSource.columns, 3, globalVarsSource.columns.length);
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
        lblGlobalVars = new JLabel();
        jLabel1 = new JLabel();
        spnVars = new JScrollPane();
        tblVars = new JTable();
        pnlFooter = new JPanel();
        pnlFooterLeft = new JPanel();
        btnAddEndPoint = new JButton();
        btnAddVar = new JButton();
        btnAddEnv = new JButton();
        pnlFooterRight = new JPanel();
        btnApply = new JButton();
        btnCancel = new JButton();

        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        setLayout(new BorderLayout(0, 5));

        pnlTop.setLayout(new BorderLayout());

        lblGlobalVars.setFont(new Font("Tahoma", 1, 11)); // NOI18N
        ResourceBundle bundle = ResourceBundle.getBundle("com/equinix/amphibia/messages"); // NOI18N
        lblGlobalVars.setText(bundle.getString("globalVars")); // NOI18N
        pnlTop.add(lblGlobalVars, BorderLayout.WEST);

        jLabel1.setForeground(new Color(51, 51, 255));
        jLabel1.setText(bundle.getString("tip_edit_global")); // NOI18N
        pnlTop.add(jLabel1, BorderLayout.EAST);

        add(pnlTop, BorderLayout.NORTH);

        tblVars.setModel(globalVarsModel);
        spnVars.setViewportView(tblVars);

        add(spnVars, BorderLayout.CENTER);

        pnlFooter.setLayout(new BorderLayout());

        pnlFooterLeft.setLayout(new FlowLayout(FlowLayout.LEFT));

        btnAddEndPoint.setIcon(new ImageIcon(getClass().getResource("/com/equinix/amphibia/icons/endpoint_16.png"))); // NOI18N
        btnAddEndPoint.setText(bundle.getString("addEndpoint")); // NOI18N
        btnAddEndPoint.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                btnAddEndPointActionPerformed(evt);
            }
        });
        pnlFooterLeft.add(btnAddEndPoint);

        btnAddVar.setIcon(new ImageIcon(getClass().getResource("/com/equinix/amphibia/icons/variable_16.png"))); // NOI18N
        btnAddVar.setText(bundle.getString("addVariable")); // NOI18N
        btnAddVar.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                btnAddVarActionPerformed(evt);
            }
        });
        pnlFooterLeft.add(btnAddVar);

        btnAddEnv.setIcon(new ImageIcon(getClass().getResource("/com/equinix/amphibia/icons/environment_16.png"))); // NOI18N
        btnAddEnv.setText(bundle.getString("addEnvironment")); // NOI18N
        btnAddEnv.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                btnAddEnvActionPerformed(evt);
            }
        });
        pnlFooterLeft.add(btnAddEnv);

        pnlFooter.add(pnlFooterLeft, BorderLayout.WEST);

        btnApply.setText(bundle.getString("apply")); // NOI18N
        btnApply.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                btnApplyActionPerformed(evt);
            }
        });
        pnlFooterRight.add(btnApply);

        btnCancel.setText(bundle.getString("cancel")); // NOI18N
        btnCancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                btnCancelActionPerformed(evt);
            }
        });
        pnlFooterRight.add(btnCancel);

        pnlFooter.add(pnlFooterRight, BorderLayout.EAST);

        add(pnlFooter, BorderLayout.SOUTH);
    }// </editor-fold>//GEN-END:initComponents

    private void btnAddEndPointActionPerformed(ActionEvent evt) {//GEN-FIRST:event_btnAddEndPointActionPerformed
        String name = Amphibia.instance.inputDialog("tip_add_var", "", new String[]{}, dialog.getParent());
        if (name != null) {
            globalVarsModel.addRow(new Object[] {0, name, "http://"});
        }
    }//GEN-LAST:event_btnAddEndPointActionPerformed

    private void btnAddVarActionPerformed(ActionEvent evt) {//GEN-FIRST:event_btnAddVarActionPerformed
        String name = Amphibia.instance.inputDialog("tip_add_var", "", new String[]{}, dialog.getParent());
        if (name != null) {
            globalVarsModel.addRow(new Object[] {1, name});
        }
    }//GEN-LAST:event_btnAddVarActionPerformed

    private void btnAddEnvActionPerformed(ActionEvent evt) {//GEN-FIRST:event_btnAddEnvActionPerformed
        String name = Amphibia.instance.inputDialog("tip_add_env", "", new String[]{}, dialog.getParent());
        if (name != null) {
            userPreferences.put(Amphibia.P_SELECTED_ENVIRONMENT, name);
            globalVarsModel.addRow(new Object[] {1, name});
        }
    }//GEN-LAST:event_btnAddEnvActionPerformed

    @SuppressWarnings("UseOfObsoleteCollectionType")
    private void btnApplyActionPerformed(ActionEvent evt) {//GEN-FIRST:event_btnApplyActionPerformed
        try {
            Object[][] data = new Object[globalVarsModel.getRowCount()][];
            java.util.Vector<java.util.Vector> rows = globalVarsModel.getDataVector();
            for (int r = 0; r < rows.size(); r++) {
                java.util.Vector row = rows.get(r);
                Object[] colData = new Object[globalVarsSource.columns.length];
                for (int c = 0; c < row.size(); c++) {
                    colData[c] = row.get(c);
                    if (colData[c] instanceof String) {
                        if ("true".equals(colData[c]) || "false".equals(colData[c])) {
                            colData[c] = Boolean.getBoolean(colData[c].toString());
                        } else {
                            try {
                                colData[c] = NUMBER.parse(String.valueOf(row.get(c)));
                            } catch (ParseException e) {
                                try {
                                    colData[c] = IO.prettyJson(colData[c].toString());
                                } catch (Exception ex) {}
                            }
                        }
                    }
                }
                data[r] = colData;
            }
            globalVarsSource.columns[1] = globalVarsSource.columns[2] = "";
            globalVarsSource.data = data;
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ObjectOutputStream os = new ObjectOutputStream(out);
            os.writeObject(globalVarsSource);
            userPreferences.putByteArray(Amphibia.P_GLOBAL_VARS, out.toByteArray());
            dialog.setVisible(false);
        } catch (IOException ex) {
            mainPanel.addError(ex);
        }
    }//GEN-LAST:event_btnApplyActionPerformed

    private void btnCancelActionPerformed(ActionEvent evt) {//GEN-FIRST:event_btnCancelActionPerformed
        dialog.setVisible(false);
    }//GEN-LAST:event_btnCancelActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private JButton btnAddEndPoint;
    private JButton btnAddEnv;
    private JButton btnAddVar;
    private JButton btnApply;
    private JButton btnCancel;
    private JLabel jLabel1;
    private JLabel lblGlobalVars;
    private JPanel pnlFooter;
    private JPanel pnlFooterLeft;
    private JPanel pnlFooterRight;
    private JPanel pnlTop;
    private JScrollPane spnVars;
    private JTable tblVars;
    // End of variables declaration//GEN-END:variables
}

final class GlobalVarModel implements Serializable {

    public String[] columns;
    public Object[][] data;

    public GlobalVarModel() {
        columns = new String[]{"", "", "", "TEST HELLO WORLD"};
        data = new Object[][]{
            new Object[]{0, "EndPoint", "http://", "http://"}
        };
    }
}
