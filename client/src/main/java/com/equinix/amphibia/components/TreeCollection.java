/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.equinix.amphibia.components;

import static com.equinix.amphibia.components.JTreeTable.EditValueRenderer.TYPE.*;
import static com.equinix.amphibia.components.TreeCollection.TYPE.*;

import com.equinix.amphibia.Amphibia;

import java.io.File;
import java.util.Enumeration;
import java.util.ResourceBundle;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import net.sf.json.JSONObject;

/**
 *
 * @author dgofman
 */
@SuppressWarnings("NonPublicExported")
public final class TreeCollection {

    private File projectFile;
    private JSONObject projectProfile;

    public final TreeIconNode project;
    public final TreeIconNode resources;
    public final TreeIconNode interfaces;
    public final TreeIconNode profile;
    public final TreeIconNode testsuites;
    public final TreeIconNode tests;
    public final TreeIconNode schemas;
    public final TreeIconNode requests;
    public final TreeIconNode responses;

    public static enum TYPE {
        ROOT,
        PROJECT,
        RESOURCES,
        TESTCASES,
        TESTSUITE,
        TESTS,
        TEST_ITEM,
        TEST_STEP_ITEM,
        SCHEMAS,
        SCHEMA_ITEM,
        REQUESTS,
        REQUEST_ITEM,
        RESPONSES,
        RESPONSE_ITEM,
        PROFILE,
        INTERFACES,
        INTERFACE,
        SWAGGER,
        WIZARD,
        RULES,
        TESTCASE,
        ERRORS
    };

    public static final Object[][] PROJECT_PROPERTIES = new Object[][]{
        {"hosts", null, VIEW},
        {"interfaces", null, VIEW},
        {"globals", null, VIEW},
        {"properties", ADD}
    };
    
    public static final Object[][] INTERFACES_PROPERTIES = new Object[][]{
        {null, new Object[][] {
            {"type", VIEW},
            {"name", VIEW},
            {"basePath", VIEW},
            {"headers", null, VIEW},
        }}
    };

    public static final Object[][] RESOURCE_PROPERTIES = new Object[][]{
        {"id", VIEW},
        {"source", VIEW},
        {"properties", VIEW},
        {"interface", VIEW}
    };

    public static final Object[][] INTERFACE_PROPERTIES = new Object[][]{
        {"name", EDIT_LIMIT},
        {"basePath", EDIT_LIMIT},
        {"type", VIEW},
        {"headers", ADD}
    };

    public static final Object[][] RULES_PROPERTIES = new Object[][]{
        {"headers", ADD},
        {"globalProperties", ADD},
        {"projectProperties", ADD},
        {"testSuiteProperties", ADD},
        {"asserts", null, VIEW}
    };

    public static final Object[][] PROFILE_PROPERTIES = new Object[][]{
        {"project", new Object[][] {
            {"id", VIEW},
            {"name", EDIT_LIMIT},
            {"appendLogs", EDIT_LIMIT},
            {"continueOnError", EDIT_LIMIT},
            {"testCaseTimeout", EDIT_LIMIT}
        }},
        {"resources", null, VIEW},
        {"testsuites", null, VIEW}
    };

    public static final Object[][] TESTSUITE_PROPERTIES = new Object[][]{
        {"disabled", EDIT},
        {"name", VIEW},
        {"endpoint", VIEW},
        {"interface", VIEW},
        {"properties", ADD},
        {"testcases", ADD_RESOURCES, REFERENCE_EDIT}
    };
    
    public static final Object[][] TESTCASE_PROPERTIES = new Object[][]{
        {"disabled", EDIT_LIMIT},
        {"name", EDIT_LIMIT},
        {"path", REFERENCE},
        {"method", VIEW},
        {"url", VIEW},
        {"interface", VIEW},
        {"transfer", TRANSFER, EDIT},
        {"headers", ADD},
        {"properties", ADD},
        {"inherited-properties", null, VIEW},
        {"teststeps", ADD_RESOURCES, REFERENCE_EDIT}
    };

    public static final Object[][] TEST_STEP_ITEM_PROPERTIES = new Object[][]{
        {"disabled", EDIT_LIMIT},
        {"name", EDIT_LIMIT},
        {"path", REFERENCE},
        {"method", VIEW},
        {"url", VIEW},
        {"request", new Object[][]{
            {"properties", ADD},
            {"body", REFERENCE},
            {"schema", REFERENCE_EDIT}
        }},
        {"response", new Object[][]{
            {"transfer", TRANSFER, EDIT},
            {"properties", ADD},
            {"body", REFERENCE},
            {"schema", REFERENCE_EDIT},
            {"asserts", REFERENCE_EDIT}
        }},
        {"inherited-properties", null, VIEW}
    };
                
    public static final Object[][] TEST_ITEM_PROPERTIES = new Object[][]{
        {"defaultName", EDIT_LIMIT},
        {"path", REFERENCE_EDIT},
        {"request", new Object[][]{
            {"properties", ADD},
            {"body", REFERENCE_EDIT},
            {"schema", REFERENCE_EDIT}
        }},
        {"response", new Object[][]{
            {"properties", ADD},
            {"body", REFERENCE_EDIT},
            {"schema", REFERENCE_EDIT},
            {"asserts", REFERENCE_EDIT}
        }}
    };
    
    public static final Object[][] TEST_TESTSUITE_PROPERTIES = new Object[][]{
        {"name", VIEW},
        {"path", VIEW},
        {"endpoint", VIEW},
        {"interface", VIEW},
        {"properties", ADD},
        {"testcases", null, VIEW}
    };
    
    public static final Object[][] TEST_TESTCASE_PROPERTIES = new Object[][]{
        {"name", VIEW},
        {"type", VIEW},
        {"summary", EDIT},
        {"operationId", EDIT},
        {"method", EDIT_LIMIT},
        {"path", EDIT_LIMIT},
        {"example", EDIT},
        {"properties", ADD},
        {"headers", ADD}
    };
                
    public static final Object[][] RESOURCES_PROPERTIES = new Object[][]{
        {"path", VIEW},
        {"files", null, VIEW}
    };

    public static final Object[][] EDIT_ITEM_PROPERTIES = new Object[][]{
        {null, EDIT}
    };

    public static final Object[][] VIEW_ITEM_PROPERTIES = new Object[][]{
        {null, VIEW}
    };

    public static TYPE getType(String type) {
        return TYPE.valueOf(type);
    }

    public TreeCollection() {
        ResourceBundle bundle = Amphibia.getBundle();
        project = new TreeIconNode(this, bundle.getString("project"), PROJECT, false, PROJECT_PROPERTIES);
        resources = new TreeIconNode(this, bundle.getString("resources"), RESOURCES, false, RESOURCE_PROPERTIES);
        interfaces = new TreeIconNode(this, bundle.getString("interfaces"), INTERFACES, false, INTERFACES_PROPERTIES);
        profile = new TreeIconNode(this, bundle.getString("profile"), PROFILE, false, PROFILE_PROPERTIES);
        testsuites = new TreeIconNode(this, bundle.getString("testcases"), TESTCASES, false);
        tests = new TreeIconNode(this, bundle.getString("tests"), TESTS, false);
        schemas = new TreeIconNode(this, bundle.getString("schemas"), SCHEMAS, false);
        requests = new TreeIconNode(this, bundle.getString("requests"), REQUESTS, false);
        responses = new TreeIconNode(this, bundle.getString("responses"), RESPONSES, false);
    }

    public TreeIconNode.TreeIconUserObject getUserObject(TreeNode node) {
        if (node instanceof TreeIconNode) {
            return ((TreeIconNode) node).getTreeIconUserObject();
        }
        return null;
    }

    public TreeIconNode addTreeNode(TreeIconNode parent, String label, TYPE type, boolean truncate) {
        TreeIconNode node = new TreeIconNode(this, label, type, truncate);
        parent.add(node);
        return node;
    }

    public TreeIconNode insertTreeNode(TreeIconNode parent, String label, TYPE type) {
        TreeIconNode node = addTreeNode(parent, label, null, false);
        node.getTreeIconUserObject().setType(type);
        return node;
    }

    public TreeIconNode expandNode(JTree tree, TreeIconNode node) {
        if (project.getTreeIconUserObject().isEnabled()) {
            tree.expandPath(new TreePath(node.getPath()));
        }
        return node;
    }

    public TreeIconNode collapseNode(JTree tree, TreeIconNode node) {
        tree.collapsePath(new TreePath(node.getPath()));
        return node;
    }

    public void reset(DefaultTreeModel treeModel) {
        removeAllChildren(treeModel, resources);
        removeAllChildren(treeModel, profile);
        removeAllChildren(treeModel, interfaces);
        removeAllChildren(treeModel, testsuites);
        removeAllChildren(treeModel, tests);
        removeAllChildren(treeModel, schemas);
        removeAllChildren(treeModel, requests);
        removeAllChildren(treeModel, responses);
    }

    public void removeAllChildren(DefaultTreeModel treeModel, DefaultMutableTreeNode node) {
        Enumeration nodes = node.children();
        while (nodes.hasMoreElements()) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) nodes.nextElement();
            removeAllChildren(treeModel, child);
            nodes = node.children();
        }
        if (node.getParent() != null) {
            node.removeFromParent();
        }
    }

    public TreeIconNode findNodeByName(TreeIconNode parent, String name) {
        for (int i = 0; i < parent.getChildCount(); i++) {
            TreeIconNode node = (TreeIconNode) parent.getChildAt(i);
            if (name.equals(node.toString())) {
                return node;
            }
        }
        return null;
    }

    public File getProjectDir() {
        return projectFile.getParentFile();
    }
     
    public JSONObject getProject() {
        return projectProfile.getJSONObject("project");
    }
    
    public String getProjectName() {
        return getProject().getString("name");
    }

    public String getUUID() {
        return getProject().getString("id");
    }
    
    public JSONObject getProjectProfile() {
        return projectProfile;
    }

    public void setProjectProfile(JSONObject json) {
        projectProfile = json;
    }

    public boolean isOpen() {
        return project.info.states.getInt(TreeIconNode.STATE_IS_OPENED) == 1;
    }

    public void setOpen(boolean isOpen) {
        project.info.states.set(TreeIconNode.STATE_IS_OPENED, isOpen ? 1 : 0);
    }

    public void setProjectFile(File file) {
        projectFile = file;
    }
    
    public File getProjectFile() {
        return projectFile;
    }
}