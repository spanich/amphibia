/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.equinix.amphibia.components;

import com.equinix.amphibia.Amphibia;
import static com.equinix.amphibia.Amphibia.P_PROJECT_UUIDS;
import static com.equinix.amphibia.Amphibia.userPreferences;
import com.equinix.amphibia.IO;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.io.File;
import java.io.IOException;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import net.sf.json.JSONArray;

/**
 *
 * @author dgofman
 */
public class SaveDialog extends javax.swing.JPanel {
    
    private JDialog dialog;
    private MainPanel mainPanel;
    private DefaultListModel model;
    private boolean isSame;

    private static final Logger logger = Logger.getLogger(SaveDialog.class.getName());
    
    /**
     * Creates new form SaveDialog
     */
    public SaveDialog(MainPanel mainPanel) {
        this.mainPanel = mainPanel;
        
        model = new DefaultListModel();
        
        initComponents();
        
        lstFiles.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                JList l = (JList)e.getSource();
                int index = l.locationToIndex(e.getPoint());
                if(index > -1) {
                    l.setToolTipText(((Item)model.getElementAt(index)).target.getAbsolutePath());
                }
            }
        });
    }
    
    public boolean validateAndSave(String title, boolean isRestore) {
        isSame = true;
        model.removeAllElements();
        JSONArray list = JSONArray.fromObject(userPreferences.get(P_PROJECT_UUIDS, "[]"));
        list.forEach((path) -> {
            try {
                File dir = new File(path.toString()).getParentFile();
                File source = new File(dir, "data/profile.bak");
                File target = new File(dir, "data/profile.json");
                if (source.exists() && target.exists() && !IO.readFile(source).equals(IO.readFile(target))) {
                    model.addElement(new Item(dir.getName() + "/data/profile.json", source, target));
                }
            } catch (IOException ex) {
                logger.log(Level.SEVERE, null, ex);
            }
        });

        if (model.getSize() > 0) {
            isSame = false;
            lstFiles.setSelectedIndex(0);
            dialog = Amphibia.createDialog(new JOptionPane(this), title, false);
            dialog.setSize(new Dimension(500, 240));
            java.awt.EventQueue.invokeLater(() -> {
                dialog.setLocationRelativeTo(mainPanel);
            });
            dialog.setVisible(true);
        }
        if (isSame && !isRestore) { //try to delete all backup files
            list.forEach((path) -> {
               File bak = new File(new File(path.toString()).getParentFile(), "data/profile.bak");
               if (bak.exists()) {
                    bak.deleteOnExit();
                    bak.delete();
               }
            });
        }
        return isSame;
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        splFiles = new JScrollPane();
        lstFiles = new JList<>();
        pnlButtons = new JPanel();
        btnSave = new JButton();
        btnSaveAll = new JButton();
        btnDiscardAll = new JButton();
        spr1 = new JSeparator();
        btnCancel = new JButton();

        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        setLayout(new BorderLayout());

        lstFiles.setModel(model);
        splFiles.setViewportView(lstFiles);

        add(splFiles, BorderLayout.CENTER);

        pnlButtons.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 0));
        pnlButtons.setLayout(new GridLayout(5, 0, 0, 10));

        ResourceBundle bundle = ResourceBundle.getBundle("com/equinix/amphibia/messages"); // NOI18N
        btnSave.setText(bundle.getString("save")); // NOI18N
        btnSave.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                btnSaveActionPerformed(evt);
            }
        });
        pnlButtons.add(btnSave);

        btnSaveAll.setText(bundle.getString("saveAll")); // NOI18N
        btnSaveAll.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                btnSaveAllActionPerformed(evt);
            }
        });
        pnlButtons.add(btnSaveAll);

        btnDiscardAll.setText(bundle.getString("discardAll")); // NOI18N
        btnDiscardAll.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                btnDiscardAllActionPerformed(evt);
            }
        });
        pnlButtons.add(btnDiscardAll);
        pnlButtons.add(spr1);

        btnCancel.setText(bundle.getString("cancel")); // NOI18N
        btnCancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                btnCancelActionPerformed(evt);
            }
        });
        pnlButtons.add(btnCancel);

        add(pnlButtons, BorderLayout.LINE_END);
    }// </editor-fold>//GEN-END:initComponents

    private void btnCancelActionPerformed(ActionEvent evt) {//GEN-FIRST:event_btnCancelActionPerformed
        dialog.setVisible(false);
    }//GEN-LAST:event_btnCancelActionPerformed

    private void btnDiscardAllActionPerformed(ActionEvent evt) {//GEN-FIRST:event_btnDiscardAllActionPerformed
        for (int index = 0; index < model.getSize(); index++) {
           Item item = (Item)model.getElementAt(index);
            try {
                IO.copy(item.target, item.source);
            } catch (IOException ex) {
                logger.log(Level.SEVERE, null, ex);
            }
        }
        isSame = true;
        dialog.setVisible(false);
    }//GEN-LAST:event_btnDiscardAllActionPerformed

    private void btnSaveAllActionPerformed(ActionEvent evt) {//GEN-FIRST:event_btnSaveAllActionPerformed
        for (int index = 0; index < model.getSize(); index++) {
           Item item = (Item)model.getElementAt(index);
            try {
                IO.copy(item.source, item.target);
            } catch (IOException ex) {
                logger.log(Level.SEVERE, null, ex);
            }
        }
        isSame = true;
        dialog.setVisible(false);
    }//GEN-LAST:event_btnSaveAllActionPerformed

    private void btnSaveActionPerformed(ActionEvent evt) {//GEN-FIRST:event_btnSaveActionPerformed
        for (int index : lstFiles.getSelectedIndices()) {
           Item item = (Item)model.getElementAt(index);
            try {
                IO.copy(item.source, item.target);
            } catch (IOException ex) {
                logger.log(Level.SEVERE, null, ex);
            }
        }
        isSame = true;
        dialog.setVisible(false);
    }//GEN-LAST:event_btnSaveActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private JButton btnCancel;
    private JButton btnDiscardAll;
    private JButton btnSave;
    private JButton btnSaveAll;
    private JList<String> lstFiles;
    private JPanel pnlButtons;
    private JScrollPane splFiles;
    private JSeparator spr1;
    // End of variables declaration//GEN-END:variables

    class Item {
        public String label;
        public File source;
        public File target;
        
        public Item(String label, File source, File target) {
            this.label = label;
            this.source = source;
            this.target = target;
        }
        
        @Override
        public String toString() {
            return label;
        }
    }
}