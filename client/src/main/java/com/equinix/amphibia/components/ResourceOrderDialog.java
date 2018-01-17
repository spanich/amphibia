/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.equinix.amphibia.components;

import static com.equinix.amphibia.components.Editor.Entry;

import com.equinix.amphibia.Amphibia;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

/**
 *
 * @author dgofman
 */
public final class ResourceOrderDialog extends javax.swing.JPanel {

    private JDialog dialog;
    private JButton updateButton;
    private JButton cancelButton;
    private ResourceBundle bundle;
    private DefaultListModel resourceModel;
    private TreeCollection collection;
    
    private MainPanel mainPanel;
    private JSONArray source;

    private static final Logger logger = Logger.getLogger(ResourceOrderDialog.class.getName());

    /**
     * Creates new form TableEditDialog
     *
     * @param mainPanel
     */
    public ResourceOrderDialog(MainPanel mainPanel) {
        this.mainPanel = mainPanel;
        resourceModel = new DefaultListModel();
        initComponents();

        bundle = Amphibia.getBundle();

        updateButton = new JButton(bundle.getString("update"));
        updateButton.addActionListener((ActionEvent evt) -> {
            try {
                source.clear();
                for (int i = 0; i < resourceModel.size(); i++) {
                    source.add(((ResourceItem) resourceModel.getElementAt(i)).json);
                }
                mainPanel.saveNodeValue(collection.profile);
                dialog.setVisible(false);
            } catch (Exception ex) {
                logger.log(Level.SEVERE, ex.toString(), ex);
            }
        });
        cancelButton = new JButton(bundle.getString("cancel"));
        cancelButton.addActionListener((ActionEvent evt) -> {
            dialog.setVisible(false);
        });

        dialog = Amphibia.createDialog(this, new Object[]{updateButton, cancelButton}, true);
        dialog.setSize(new Dimension(700, 400));
        java.awt.EventQueue.invokeLater(() -> {
            dialog.setLocationRelativeTo(mainPanel);
        });
    }

    @SuppressWarnings("NonPublicExported")
    public void openDialog(TreeIconNode node, Entry entry, int index) {
        this.collection = node.getCollection();
        resourceModel.removeAllElements();
        if (node.getType() == TreeCollection.TYPE.TESTSUITE) {
            source = MainPanel.selectedNode.info.testSuite.getJSONArray("testcases");
        } else {
            source = MainPanel.selectedNode.info.testCase.getJSONArray("steps");
        }
        source.forEach((item) -> {
            ResourceItem element = new ResourceItem((JSONObject)item);
            resourceModel.addElement(element);
        });
        lstResource.setSelectedIndex(index);
        txtName.setText(MainPanel.selectedNode.toString());
        dialog.setVisible(true);
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

        pnlHeader = new JPanel();
        lblName = new JLabel();
        txtName = new JTextField();
        pnlBotton = new JPanel();
        splResource = new JScrollPane();
        lstResource = new JList<>();
        pnlLeft = new JPanel();
        pnlButtons = new JPanel();
        btnUp = new JButton();
        btnDown = new JButton();
        fltSpace = new Box.Filler(new Dimension(0, 0), new Dimension(0, 0), new Dimension(32767, 0));
        btnClone = new JButton();
        btnRemove = new JButton();

        setBorder(BorderFactory.createEmptyBorder(5, 10, 0, 10));
        setLayout(new BorderLayout());

        pnlHeader.setBorder(BorderFactory.createEmptyBorder(1, 1, 5, 1));
        pnlHeader.setLayout(new GridBagLayout());

        lblName.setFont(new Font("Tahoma", 1, 11)); // NOI18N
        ResourceBundle bundle = ResourceBundle.getBundle("com/equinix/amphibia/messages"); // NOI18N
        lblName.setText(bundle.getString("name")); // NOI18N
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridwidth = GridBagConstraints.RELATIVE;
        gridBagConstraints.ipadx = 5;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.insets = new Insets(0, 0, 5, 0);
        pnlHeader.add(lblName, gridBagConstraints);

        txtName.setEditable(false);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new Insets(0, 0, 5, 0);
        pnlHeader.add(txtName, gridBagConstraints);

        add(pnlHeader, BorderLayout.NORTH);

        pnlBotton.setLayout(new BorderLayout());

        lstResource.setModel(this.resourceModel);
        lstResource.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        splResource.setViewportView(lstResource);

        pnlBotton.add(splResource, BorderLayout.CENTER);

        pnlButtons.setLayout(new GridLayout(5, 0, 0, 4));

        btnUp.setText(bundle.getString("up")); // NOI18N
        btnUp.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                btnUpActionPerformed(evt);
            }
        });
        pnlButtons.add(btnUp);

        btnDown.setText(bundle.getString("down")); // NOI18N
        btnDown.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                btnDownActionPerformed(evt);
            }
        });
        pnlButtons.add(btnDown);
        pnlButtons.add(fltSpace);

        btnClone.setText(bundle.getString("clone")); // NOI18N
        btnClone.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                btnCloneActionPerformed(evt);
            }
        });
        pnlButtons.add(btnClone);

        btnRemove.setText(bundle.getString("remove")); // NOI18N
        btnRemove.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                btnRemoveActionPerformed(evt);
            }
        });
        pnlButtons.add(btnRemove);

        pnlLeft.add(pnlButtons);

        pnlBotton.add(pnlLeft, BorderLayout.LINE_END);

        add(pnlBotton, BorderLayout.CENTER);
    }// </editor-fold>//GEN-END:initComponents

    private void btnDownActionPerformed(ActionEvent evt) {//GEN-FIRST:event_btnDownActionPerformed
        int index = lstResource.getSelectedIndex();
        if (index != -1 && index < resourceModel.size() - 1) {
            resourceModel.add(index + 2, resourceModel.getElementAt(index));
            lstResource.setSelectedIndex(index + 2);
            resourceModel.remove(index);
        }
    }//GEN-LAST:event_btnDownActionPerformed

    private void btnUpActionPerformed(ActionEvent evt) {//GEN-FIRST:event_btnUpActionPerformed
        int index = lstResource.getSelectedIndex();
        if (index != -1 && index > 0) {
            resourceModel.add(index - 1, resourceModel.getElementAt(index));
            lstResource.setSelectedIndex(index - 1);
            resourceModel.remove(index + 1);
        }
    }//GEN-LAST:event_btnUpActionPerformed

    private void btnRemoveActionPerformed(ActionEvent evt) {//GEN-FIRST:event_btnRemoveActionPerformed
        int index = lstResource.getSelectedIndex();
        if (index != -1) {
            resourceModel.remove(index);
        }
    }//GEN-LAST:event_btnRemoveActionPerformed

    private void btnCloneActionPerformed(ActionEvent evt) {//GEN-FIRST:event_btnCloneActionPerformed
        int index = lstResource.getSelectedIndex();
        JSONObject json = ((ResourceItem) resourceModel.getElementAt(index)).json;
        source.add(json);
        mainPanel.saveNodeValue(collection.profile);
        dialog.setVisible(false);
    }//GEN-LAST:event_btnCloneActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private JButton btnClone;
    private JButton btnDown;
    private JButton btnRemove;
    private JButton btnUp;
    private Box.Filler fltSpace;
    private JLabel lblName;
    private JList<String> lstResource;
    private JPanel pnlBotton;
    private JPanel pnlButtons;
    private JPanel pnlHeader;
    private JPanel pnlLeft;
    private JScrollPane splResource;
    private JTextField txtName;
    // End of variables declaration//GEN-END:variables

    class ResourceItem {
    
        public String label;
        public JSONObject json;

        public ResourceItem(JSONObject json) {
            this.json = json;
            this.label = json.getString("name");
        }

        @Override
        public String toString() {
            return label;
        }
    }
}