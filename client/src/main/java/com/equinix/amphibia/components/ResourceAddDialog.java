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
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Enumeration;
import java.util.ResourceBundle;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.SwingConstants;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

/**
 *
 * @author dgofman
 */
public final class ResourceAddDialog extends javax.swing.JPanel {

    private final DefaultTreeModel model;
    private final TreeIconNode testRootNode;
    private TreeIconNode selectedNode;
    private JDialog dialog;
    private JButton okButton;
    private JButton cancelButton;

    private MainPanel mainPanel;
    private ResourceBundle bundle;

    private static final Logger logger = Logger.getLogger(ResourceAddDialog.class.getName());

    /**
     * Creates new form JDialogTest
     *
     * @param mainPanel
     */
    public ResourceAddDialog(MainPanel mainPanel) {
        this.mainPanel = mainPanel;
        
        bundle = Amphibia.getBundle();
        
        testRootNode = new TreeIconNode(null, bundle.getString("tests"), TreeCollection.TYPE.TESTS, false);
        model = new DefaultTreeModel(testRootNode);

        initComponents();

        treeTests.setModel(model);
        treeTests.setRowHeight(20);
        treeTests.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        treeTests.setCellRenderer(new DefaultTreeCellRenderer() {
            @Override
            public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean isLeaf, int row, boolean hasFocus) {
                Object userObject = ((DefaultMutableTreeNode) value).getUserObject();
                Component c = super.getTreeCellRendererComponent(tree, value, selected, expanded, isLeaf, row, hasFocus);
                if (userObject instanceof TreeIconNode.TreeIconUserObject) {
                    TreeIconNode.TreeIconUserObject node = (TreeIconNode.TreeIconUserObject) userObject;
                    setToolTipText(node.getTooltip());
                    setIcon(node.getIcon());
                }
                return c;
            }
        });
        treeTests.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                TreePath path = treeTests.getPathForLocation(e.getX(), e.getY());
                if (path == null) {
                    return;
                }
                TreeIconNode node = (TreeIconNode) treeTests.getLastSelectedPathComponent();
                if (node != null && node.getType() == TreeCollection.TYPE.TESTCASE) {
                    selectedNode = node;
                    txtName.setText(node.getLabel());
                }
            }
        });
        ToolTipManager tooltip = ToolTipManager.sharedInstance();
        tooltip.setInitialDelay(200);
        tooltip.registerComponent(treeTests);

        okButton = new JButton(UIManager.getString("OptionPane.okButtonText"));
        okButton.addActionListener((ActionEvent evt) -> {
            TreeCollection collection = MainPanel.selectedNode.getCollection();
            if (MainPanel.selectedNode.getType() == TreeCollection.TYPE.TESTSUITE) {
                if (selectedNode == null) {
                    return;
                }
                JSONArray testcases = MainPanel.selectedNode.info.testSuite.getJSONArray("testcases");
                for (Object testcase : testcases) {
                    if (txtName.getText().equals(((JSONObject) testcase).getString("name"))) {
                        lblError.setText(bundle.getString("tip_name_exists"));
                        lblError.setVisible(true);
                        return;
                    }
                }
                String fileFormat = "data/%s/tests/%s/%s.json";
                String path = String.format(fileFormat, selectedNode.info.resource.getString("resourceId"), selectedNode.getParent().toString(), selectedNode.toString());
                if (IO.getFile(collection, path).exists()) { 
                    JSONObject testcase = new JSONObject();
                    testcase.put("name", txtName.getText());
                    testcase.put("path", path);
                    testcase.put("steps", new JSONArray());
                    testcases.add(testcase);
                } else {
                    lblError.setText(String.format(bundle.getString("error_missing_file"), path));
                    lblError.setToolTipText(lblError.getText());
                    lblError.setVisible(true);
                    return;
                }
            }
            mainPanel.saveNodeValue(collection.profile);
            dialog.setVisible(false);
        });
        cancelButton = new JButton(UIManager.getString("OptionPane.cancelButtonText"));
        cancelButton.addActionListener((ActionEvent evt) -> {
            dialog.setVisible(false);
        });

        dialog = Amphibia.createDialog(this, new Object[]{okButton, cancelButton}, true);
        dialog.setSize(new Dimension(400, 400));
        java.awt.EventQueue.invokeLater(() -> {
            dialog.setLocationRelativeTo(mainPanel);
        });
    }
    
    public void showTestCaseDialog(TreeIconNode selectedNode) {
        this.selectedNode = null; //reset last seletion
        txtName.setText("");
        lblError.setVisible(false);
        testRootNode.removeAllChildren();
        Enumeration<TreeIconNode> tests = selectedNode.getCollection().tests.children();
        while (tests.hasMoreElements()) {
            TreeIconNode node = tests.nextElement().cloneNode();
            Enumeration<TreeIconNode> testcases = node.getSource().children();
            while (testcases.hasMoreElements()) {
                node.add(testcases.nextElement().cloneNode());
            }
            testRootNode.add(node);
        }
        model.setRoot(testRootNode);
        dialog.setVisible(true);
    }
    
    @SuppressWarnings("NonPublicExported")
    public void showTestStepDialog(TreeIconNode selectedNode) {
        JSONArray steps = selectedNode.info.testCase.getJSONArray("steps");
        String[] names = new String[steps.size()];
        for (int i = 0; i < steps.size(); i++) {
            names[i] = steps.getJSONObject(i).getString("name");
        }
        String name = null;
        if (selectedNode.info.testStepInfo != null) {
            name = selectedNode.info.testStepInfo.getString("defaultName");
        }
        name = Amphibia.instance.inputDialog("testStepName", name, names);
        if (name != null && !name.isEmpty()) {
            TreeIconNode node = selectedNode.getCollection().insertTreeNode(selectedNode, name, TreeCollection.TYPE.TEST_STEP_ITEM);
            node.saveSelection();
            JSONObject json = new JSONObject();
            json.element("name", name);
            steps.add(json);
            mainPanel.saveNodeValue((TreeIconNode.ProfileNode)selectedNode.getCollection().profile);
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

        slpTests = new JScrollPane();
        treeTests = new JTree();
        pnlBottom = new JPanel();
        lblName = new JLabel();
        txtName = new JTextField();
        lblError = new JLabel();

        setLayout(new BorderLayout());

        treeTests.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        slpTests.setViewportView(treeTests);

        add(slpTests, BorderLayout.CENTER);

        pnlBottom.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        pnlBottom.setLayout(new BorderLayout(5, 0));

        ResourceBundle bundle = ResourceBundle.getBundle("com/equinix/amphibia/messages"); // NOI18N
        lblName.setText(bundle.getString("name")); // NOI18N
        pnlBottom.add(lblName, BorderLayout.WEST);
        pnlBottom.add(txtName, BorderLayout.CENTER);

        lblError.setForeground(Color.red);
        lblError.setHorizontalAlignment(SwingConstants.CENTER);
        lblError.setText(bundle.getString("tip_name_exists")); // NOI18N
        lblError.setInheritsPopupMenu(false);
        pnlBottom.add(lblError, BorderLayout.PAGE_END);

        add(pnlBottom, BorderLayout.PAGE_END);
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private JLabel lblError;
    private JLabel lblName;
    private JPanel pnlBottom;
    private JScrollPane slpTests;
    private JTree treeTests;
    private JTextField txtName;
    // End of variables declaration//GEN-END:variables
}
