/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.equinix.amphibia.components;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ResourceBundle;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

/**
 *
 * @author dgofman
 */
public class WizardTab extends javax.swing.JPanel {
    
    private Wizard wizard;
    
    /**
     * Creates new form WizardTab
     */
    public WizardTab() {
        initComponents();
    }
    
    public void setWizard(Wizard wizard) {
        this.wizard = wizard;
        this.cmdEnv.setModel(wizard.envModel);
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

        pnlTop = new JPanel();
        lblEnv = new JLabel();
        pnlEnv = new JPanel();
        cmdEnv = new JComboBox<>();
        btnEnvInfo = new JButton();
        lblMethod = new JLabel();
        pnlMethodURI = new JPanel();
        cmdMethod = new JComboBox<>();
        lblURI = new JLabel();
        lblPath = new JLabel();
        txtPath = new JTextField();
        btnHeaders = new JButton();
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

        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        setLayout(new BorderLayout());

        pnlTop.setLayout(new GridBagLayout());

        ResourceBundle bundle = ResourceBundle.getBundle("com/equinix/amphibia/messages"); // NOI18N
        lblEnv.setText(bundle.getString("environment")); // NOI18N
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        pnlTop.add(lblEnv, gridBagConstraints);

        pnlEnv.setLayout(new FlowLayout(FlowLayout.LEFT));

        cmdEnv.setPreferredSize(new Dimension(250, 20));
        cmdEnv.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                cmdEnvActionPerformed(evt);
            }
        });
        pnlEnv.add(cmdEnv);

        btnEnvInfo.setIcon(new ImageIcon(getClass().getResource("/com/equinix/amphibia/icons/icon-16-info.png"))); // NOI18N
        btnEnvInfo.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnEnvInfo.setPreferredSize(new Dimension(30, 22));
        btnEnvInfo.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                btnEnvInfoActionPerformed(evt);
            }
        });
        pnlEnv.add(btnEnvInfo);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        pnlTop.add(pnlEnv, gridBagConstraints);

        lblMethod.setText(bundle.getString("method")); // NOI18N
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        pnlTop.add(lblMethod, gridBagConstraints);

        pnlMethodURI.setLayout(new BorderLayout(15, 0));

        cmdMethod.setModel(new DefaultComboBoxModel<>(new String[] { "GET", "POST", "DELETE", "PUT", "PATCH", "OPTIONS" }));
        pnlMethodURI.add(cmdMethod, BorderLayout.WEST);

        lblURI.setText("http://");
        pnlMethodURI.add(lblURI, BorderLayout.CENTER);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        pnlTop.add(pnlMethodURI, gridBagConstraints);

        lblPath.setText(bundle.getString("path")); // NOI18N
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        pnlTop.add(lblPath, gridBagConstraints);

        txtPath.setEditable(false);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridy = 3;
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
        gridBagConstraints.gridy = 3;
        pnlTop.add(btnHeaders, gridBagConstraints);

        add(pnlTop, BorderLayout.NORTH);

        txtReqBody.setColumns(20);
        txtReqBody.setRows(5);
        spnReqBody.setViewportView(txtReqBody);

        tabBody.addTab(bundle.getString("requestBody"), spnReqBody); // NOI18N

        txtResBody.setColumns(20);
        txtResBody.setRows(5);
        spnResBody.setViewportView(txtResBody);

        tabBody.addTab(bundle.getString("responseBody"), spnResBody); // NOI18N

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
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        pnlFooter.add(btnSend, gridBagConstraints);

        btnSave.setText(bundle.getString("save")); // NOI18N
        btnSave.setEnabled(false);
        btnSave.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                btnSaveActionPerformed(evt);
            }
        });
        pnlFooter.add(btnSave, new GridBagConstraints());

        add(pnlFooter, BorderLayout.SOUTH);
    }// </editor-fold>//GEN-END:initComponents

    private void btnEnvInfoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnEnvInfoActionPerformed
        wizard.openEnvironmentPanel(this);
    }//GEN-LAST:event_btnEnvInfoActionPerformed

    private void cmdEnvActionPerformed(ActionEvent evt) {//GEN-FIRST:event_cmdEnvActionPerformed
        if (cmdEnv.getSelectedIndex() > 1) {
            
        } else {
            
        }
    }//GEN-LAST:event_cmdEnvActionPerformed

    private void btnHeadersActionPerformed(ActionEvent evt) {//GEN-FIRST:event_btnHeadersActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_btnHeadersActionPerformed

    private void btnSendActionPerformed(ActionEvent evt) {//GEN-FIRST:event_btnSendActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_btnSendActionPerformed

    private void btnSaveActionPerformed(ActionEvent evt) {//GEN-FIRST:event_btnSaveActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_btnSaveActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    JButton btnEnvInfo;
    JButton btnHeaders;
    JButton btnSave;
    JButton btnSend;
    JComboBox<String> cmdEnv;
    JComboBox<String> cmdMethod;
    JLabel lblCode;
    JLabel lblEnv;
    JLabel lblMethod;
    JLabel lblPath;
    JLabel lblStatusCode;
    JLabel lblURI;
    JPanel pnlEnv;
    JPanel pnlFooter;
    JPanel pnlMethodURI;
    JPanel pnlTop;
    JScrollPane spnConsole;
    JScrollPane spnReqBody;
    JScrollPane spnResBody;
    JTabbedPane tabBody;
    JTextArea txtConsole;
    JTextField txtPath;
    JTextArea txtReqBody;
    JTextArea txtResBody;
    // End of variables declaration//GEN-END:variables
}
