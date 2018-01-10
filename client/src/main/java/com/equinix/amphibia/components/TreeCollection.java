/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.equinix.amphibia.components;

import static com.equinix.amphibia.Amphibia.P_PROJECT;
import static com.equinix.amphibia.Amphibia.P_PROJECT_UUIDS;
import static com.equinix.amphibia.components.JTreeTable.EditValueRenderer.TYPE.*;
import static com.equinix.amphibia.components.TreeCollection.TYPE.*;
import static com.equinix.amphibia.Amphibia.getUserPreferences;

import com.equinix.amphibia.Amphibia;

import java.io.File;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

/**
 *
 * @author dgofman
 */
@SuppressWarnings("NonPublicExported")
public final class TreeCollection {

    private static final Logger logger = Logger.getLogger(TreeCollection.class.getName());
    private static final Preferences userPreferences = getUserPreferences();
    private static final JSONArray projectList = JSONArray.fromObject(userPreferences.get(P_PROJECT_UUIDS, "[]"));

    private SerializeProject serialize;

    public final TreeIconNode project;
    public final TreeIconNode swaggers;
    public final TreeIconNode runner;
    public final TreeIconNode testsuites;
    public final TreeIconNode tests;
    public final TreeIconNode schemas;
    public final TreeIconNode requests;
    public final TreeIconNode responses;

    public static enum TYPE {
        ROOT,
        PROJECT,
        SWAGGERS,
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
        RUNNERS,
        INTERFACE,
        SWAGGER,
        RULES,
        TESTCASE,
        ERRORS
    };

    public static final Object[][] PROJECT_PROPERTIES = new Object[][]{
        {"name", EDIT_LIMIT},
        {"hosts", null, VIEW},
        {"interfaces", null, VIEW},
        {"globals", null, VIEW},
        {"properties", ADD}
    };

    public static final Object[][] DOCUMENT_PROPERTIES = new Object[][]{
        {"swagger", VIEW},
        {"properties", VIEW}
    };

    public static final Object[][] INTERFACE_PROPERTIES = new Object[][]{
        {"name", EDIT_LIMIT},
        {"basePath", VIEW},
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

    public static final Object[][] RUNNERS_PROPERTIES = new Object[][]{
        {"options", null, EDIT},
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
        {"headers", ADD},
        {"properties", ADD},
        {"transfer", TRANSFER, EDIT},
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
            {"properties", ADD},
            {"body", REFERENCE},
            {"schema", REFERENCE_EDIT},
            {"asserts", REFERENCE_EDIT},
            {"transfer", TRANSFER, EDIT_LIMIT}
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
        serialize = new SerializeProject();
        ResourceBundle bundle = Amphibia.getBundle();
        project = new TreeIconNode(this, bundle.getString("project"), PROJECT, false, PROJECT_PROPERTIES);
        swaggers = new TreeIconNode(this, bundle.getString("swaggers"), SWAGGERS, false, DOCUMENT_PROPERTIES);
        runner = new TreeIconNode(this, "", RUNNERS, false, RUNNERS_PROPERTIES);
        testsuites = new TreeIconNode(this, bundle.getString("testcases"), TESTCASES, false);
        tests = new TreeIconNode(this, bundle.getString("tests"), TESTS, false);
        schemas = new TreeIconNode(this, bundle.getString("schemas"), SCHEMAS, false);
        requests = new TreeIconNode(this, bundle.getString("requests"), REQUESTS, false);
        responses = new TreeIconNode(this, bundle.getString("responses"), RESPONSES, false);
    }

    public TreeIconNode.TreeIconUserObject getUserObject(TreeNode node) {
        if (node != null) {
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
        removeAllChildren(treeModel, swaggers);
        removeAllChildren(treeModel, runner);
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

    public void setProjectName(String name) {
        serialize.projectName = name;
    }

    public boolean setProjectName(String name, JTree tree) {
        serialize.projectName = name;
        project.getTreeIconUserObject().setLabel(serialize.projectName);
        ((DefaultTreeModel) tree.getModel()).nodeChanged(project);
        tree.repaint();
        JSON json = project.getTreeIconUserObject().json;
        if (json instanceof JSONObject) {
            try {
                ((JSONObject) json).element("name", name);
                return true;
            } catch (Exception ex) {
                logger.log(Level.SEVERE, ex.toString(), ex);
            }
        }
        return false;
    }

    public String getUUID() {
        return serialize.uuid;
    }

    public void setUID(String uid) {
        serialize.uuid = uid;
    }

    public boolean isOpen() {
        return serialize.isOpen;
    }

    public void setOpen(boolean isOpen) {
        serialize.isOpen = isOpen;
    }

    public String getProjectName() {
        return serialize.projectName;
    }

    public void setProjectDir(File dir) {
        serialize.projectDir = dir;
    }

    public File getProjectDir() {
        return serialize.projectDir;
    }

    public File getProjectFile() {
        return serialize.projectFile;
    }

    public void setProjectFile(File file) {
        serialize.projectFile = file;
        serialize.projectDir = file.getParentFile();
    }

    public void save() {
        try {
            Preferences pref = getUserPreferences();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ObjectOutputStream os = new ObjectOutputStream(out);
            os.writeObject(serialize);
            pref.putByteArray(P_PROJECT + serialize.uuid, out.toByteArray());
            if (!projectList.contains(serialize.uuid)) {
                projectList.add(serialize.uuid);
            }
            pref.put(P_PROJECT_UUIDS, projectList.toString());
        } catch (IOException ex) {
            logger.log(Level.SEVERE, null, ex);
        }
    }

    public static void initializeCollections(MainPanel mainPanel, Preferences pref) {
        Map<String, Integer> createdProjects = new HashMap<>();
        for (int i = projectList.size() - 1; i >= 0; i--) {
            String uuid = projectList.getString(i);
            byte[] data = pref.getByteArray(P_PROJECT + uuid, null);
            if (data != null) {
                try {
                    TreeCollection collection = new TreeCollection();
                    collection.serialize = (SerializeProject) new ObjectInputStream(new ByteArrayInputStream(data)).readObject();
                    if (collection.getProjectFile() != null && collection.getProjectFile().exists()) {
                        if (!collection.serialize.isOpen) {
                            collection.project.getTreeIconUserObject().setEnabled(false);
                        }
                        if (mainPanel.loadProject(collection)) {
                            createdProjects.put(P_PROJECT + uuid, i);
                            continue;
                        }
                    }
                } catch (IOException | ClassNotFoundException ex) {
                    logger.log(Level.SEVERE, null, ex);
                }
            }
            projectList.remove(i);
            pref.put(P_PROJECT_UUIDS, projectList.toString());
            pref.remove(P_PROJECT + uuid);
        }
        try {
            //clean old keys
            for (String key : pref.keys()) {
                if (key.startsWith(P_PROJECT) && !createdProjects.containsKey(key)) {
                    pref.remove(key);
                }
            }
        } catch (BackingStoreException ex) {
            logger.log(Level.SEVERE, null, ex);
        }
    }
}

final class SerializeProject implements Serializable {

    public String uuid = UUID.randomUUID().toString();
    public File projectFile;
    public String projectName = "";
    public File projectDir = new File(".");
    public boolean isOpen = true;
}
