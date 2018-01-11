/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.equinix.amphibia.components;

import static com.equinix.amphibia.Amphibia.getUserPreferences;
import static com.equinix.amphibia.components.TreeCollection.*;
import static com.equinix.amphibia.components.TreeCollection.TYPE.*;

import com.equinix.amphibia.agent.converter.Swagger;
import com.equinix.amphibia.agent.builder.Properties;
import com.equinix.amphibia.Amphibia;
import com.equinix.amphibia.HistoryManager;
import com.equinix.amphibia.IO;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Component;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Timer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javax.swing.border.Border;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTree;
import javax.swing.ToolTipManager;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONNull;
import net.sf.json.JSONObject;

/**
 *
 * @author dgofman
 */
public final class MainPanel extends javax.swing.JPanel {

    public static TreeIconNode selectedNode;

    public final TreeIconNode treeNode;
    public final TreeIconNode debugTreeNode;
    
    public final TreeIconNode reportTreeNode;
    public final DefaultTreeModel treeModel;
    public final DefaultTreeModel debugTreeModel;
    public final DefaultTreeModel reportTreeModel;
    public final HistoryManager history;
    public final Runner runner;

    private Amphibia amphibia;
    private TreePopupMenuBuilder menuBuilder;

    ResourceEditDialog resourceEditDialog;
    ResourceOrderDialog resourceOrderDialog;
    ReferenceDialog referenceEditDialog;
    ResourceAddDialog resourceAddDialog;
    TransferDialog transferDialog;

    private final Preferences userPreferences = getUserPreferences();

    private static final Logger logger = Logger.getLogger(MainPanel.class.getName());

    /**
     * Creates new form MainPanel
     */
    public MainPanel() {
        treeNode = new TreeIconNode();
        treeModel = new DefaultTreeModel(treeNode);
        debugTreeNode = new TreeIconNode();
        debugTreeModel = new DefaultTreeModel(debugTreeNode);
        reportTreeNode = new TreeIconNode();
        reportTreeModel = new DefaultTreeModel(reportTreeNode);

        initComponents();

        referenceEditDialog = new ReferenceDialog(this);
        resourceEditDialog = new ResourceEditDialog(this);
        resourceAddDialog = new ResourceAddDialog(this);
        resourceOrderDialog = new ResourceOrderDialog(this);
        transferDialog = new TransferDialog(this);

        spnMainPane.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY, new PropertyChangeListener() {
            Timer timer = new Timer();

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                Amphibia.updatePreferences(timer, () -> {
                    userPreferences.putInt(Amphibia.P_DIVIDER + "Main", spnMainPane.getDividerLocation());
                });
            }
        });
        spnMainPane.setDividerLocation(userPreferences.getInt(Amphibia.P_DIVIDER + "Main", 320));

        history = new HistoryManager(this, editor);

        editor.setMainPanel(this);
        runner = new Runner(this, editor);

        final JTextArea errors = new JTextArea();
        final FontMetrics fm = getFontMetrics(errors.getFont());
        final Border border = BorderFactory.createEmptyBorder (2, 0, 2, 0);
        errors.setBorder(border);
        errors.setWrapStyleWord(true);
        errors.setLineWrap(true);
        errors.setOpaque(true);
        errors.setForeground(Color.red);
      
        DefaultTreeCellRenderer treeCellRenderer = new DefaultTreeCellRenderer() {
            @Override
            public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean isLeaf, int row, boolean hasFocus) {
                
                TreeIconNode node = ((TreeIconNode) value);
                if (node.getType() == ERRORS) {
                    int width = 0;
                    String[] lines = value.toString().split("\n");
                    for (String line : lines) {
                        width = Math.max(width, (int)fm.getStringBounds(line, errors.getGraphics()).getWidth());
                    }
                    errors.setText(value.toString());
                    errors.setSize(width + 10, Short.MAX_VALUE);
                    return errors;
                }
                JComponent c = (JComponent) super.getTreeCellRendererComponent(tree, value, selected, expanded, isLeaf, row, hasFocus);
                if (node.getUserObject() instanceof TreeIconNode.TreeIconUserObject) {
                    TreeIconNode.TreeIconUserObject userObject = (TreeIconNode.TreeIconUserObject) node.getUserObject();
                    Icon icon = userObject.getIcon();
                    Icon stateIcon;
                    if (tree == treeNav) {
                        switch (userObject.getType()) {
                            case TESTS:
                            case REQUESTS:
                            case RESPONSES:
                            case SCHEMAS:
                                c.setForeground(new Color(0, 0, 0, 80));
                                BufferedImage b = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
                                Graphics2D g = b.createGraphics();
                                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
                                icon.paintIcon(null, g, 0, 0);
                                g.dispose();
                                icon = new ImageIcon(b);
                                break;
                        }
                    } else if ((stateIcon = node.getReportIcon()) != null) {
                        if (icon != null) {
                            BufferedImage b = new BufferedImage(icon.getIconWidth() + stateIcon.getIconWidth() + 2, icon.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
                            Graphics2D g = b.createGraphics();
                            icon.paintIcon(null, g, 0, 0);
                            stateIcon.paintIcon(null, g, icon.getIconWidth() + 2, 2);
                            g.dispose();
                            icon = new ImageIcon(b);
                        } else {
                            icon = stateIcon;
                        }
                    }
                    c.setEnabled(isNodeEnabled((TreeNode) value));
                    setEnabled(userObject.isEnabled());
                    setToolTipText(userObject.getTooltip());
                    setIcon(icon);
                }
                c.setBorder(border);
                return c;
            }
        };
        TreeExpansionListener treeExpansionListener = new TreeExpansionListener() {
            @Override
            public void treeExpanded(TreeExpansionEvent event) {
                TreePath path = event.getPath();
                TreeIconNode node = (TreeIconNode) path.getLastPathComponent();
                if (!node.getTreeIconUserObject().isEnabled()) {
                    ((JTree)event.getSource()).collapsePath(path);
                } else if (!runner.isRunning() && node.info != null) {
                    TreeCollection collection = node.getCollection();
                    if (node.info.states != null) {
                        node.info.states.set(getStateIndex(event.getSource()), 1);
                    } else {
                        collection.runner.jsonObject().getJSONObject("expandResources").element(node.info.file.getAbsolutePath(), true);
                    }
                    IO.write(collection.runner, editor);
                }
            }

            @Override
            public void treeCollapsed(TreeExpansionEvent event) {
                TreePath path = event.getPath();
                TreeIconNode node = (TreeIconNode) path.getLastPathComponent();
                if (!runner.isRunning() && node.info != null) {
                    TreeCollection collection = node.getCollection();
                    JSONObject expandResources = collection.runner.jsonObject().getJSONObject("expandResources");
                    int index = getStateIndex(event.getSource());
                    if (collection.isOpen()) {
                        Enumeration children = node.preorderEnumeration();
                        while (children.hasMoreElements()) {
                            TreeIconNode child = (TreeIconNode) children.nextElement();
                            if (child.info != null) {
                                if (child.info.states != null) {
                                    child.info.states.set(index, 0);
                                } else {
                                    expandResources.remove(child.info.file.getAbsolutePath());
                                }
                            }
                        }
                    }
                    if (node.info.states != null) {
                        node.info.states.set(index, 0);
                    } else {
                        expandResources.remove(node.info.file.getAbsolutePath());
                    }
                    IO.write(collection.runner, editor);
                }
            }
        };

        treeNav.setRowHeight(20);
        treeNav.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));
        treeNav.setEditable(false);
        treeNav.setRootVisible(false);
        treeNav.setShowsRootHandles(true);
        treeNav.setCellRenderer(treeCellRenderer);
        treeNav.addTreeExpansionListener(treeExpansionListener);
        treeNav.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                TreePath path = treeNav.getPathForLocation(e.getX(), e.getY());
                if (path == null) {
                    return;
                }
                TreeIconNode selectedNode = (TreeIconNode) treeNav.getLastSelectedPathComponent();
                if (selectedNode != null) {
                    selectedNode.saveSelection();
                    TreeIconNode.TreeIconUserObject selectedUserObject = selectedNode.getTreeIconUserObject();
                    System.out.println("TYPE: " + selectedUserObject.getType());
                    if (selectedUserObject.getType() != null && e.isPopupTrigger()) {
                        JPopupMenu popup = menuBuilder.createPopupMenu(selectedNode);
                        popup.show(treeNav, e.getX(), e.getY());
                    }
                    if (selectedNode.getCollection().isOpen()) {
                        selectNode(selectedNode);
                    }
                }
            }
        });

        debugTreeNav.setRowHeight(20);
        debugTreeNav.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));
        debugTreeNav.setEditable(false);
        debugTreeNav.setRootVisible(false);
        debugTreeNav.setShowsRootHandles(true);
        debugTreeNav.setCellRenderer(treeCellRenderer);
        debugTreeNav.addTreeExpansionListener(treeExpansionListener);
        debugTreeNav.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                TreePath path = debugTreeNav.getPathForLocation(e.getX(), e.getY());
                if (path == null) {
                    return;
                }
                TreeIconNode selectedNode = (TreeIconNode) debugTreeNav.getLastSelectedPathComponent();
                if (selectedNode != null && selectedNode.source != null) {
                    selectedNode.source.saveSelection();
                    selectNode(selectedNode.source);
                    
                    if (selectedNode.info != null) {
                        editor.spnConsole.getVerticalScrollBar().setValue(selectedNode.info.consoleLine);
                    } 
                }
            }
        });

        reportTreeNav.setRowHeight(-1);
        reportTreeNav.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));
        reportTreeNav.setEditable(false);
        reportTreeNav.setRootVisible(false);
        reportTreeNav.setShowsRootHandles(true);
        reportTreeNav.setCellRenderer(treeCellRenderer);

        ToolTipManager tooltip = ToolTipManager.sharedInstance();
        tooltip.setInitialDelay(200);
        tooltip.registerComponent(treeNav);
        tooltip.registerComponent(debugTreeNav);
        tooltip.registerComponent(reportTreeNav);
    }

    public JTree getTree() {
        return treeNav;
    }

    public int getStateIndex(Object tree) {
        return tree == treeNav ? TreeIconNode.STATE_PROJECT_EXPAND : TreeIconNode.STATE_DEBUG_EXPAND;
    }

    public void deleteProject(TreeCollection collection) {
        reset(collection);
        editor.resetHistory();
        amphibia.enableSave(false);
        amphibia.tlbRun.setEnabled(false);
        treeNav.updateUI();
    }

    public void reset(TreeCollection collection) {
        collection.reset(treeModel);
        collection.project.removeFromParent();
        if (collection.project.debugNode != null) {
            collection.project.debugNode.removeAllChildren();
        }
        editor.selectedTreeNode(collection.project);
        editor.reset();
    }
    
    public void saveNodeValue(TreeIconNode node) {
        history.saveNodeValue(node);
    }

    private void openCloseProject(TreeCollection collection, boolean isOpen) {
        collection.project.getTreeIconUserObject().setEnabled(isOpen);
        collection.project.debugNode.getTreeIconUserObject().setEnabled(isOpen);
        collection.setOpen(isOpen);
        if (isOpen) {
            collection.expandNode(treeNav, collection.project);
            collection.expandNode(treeNav, collection.testsuites);
        } else {
            collection.collapseNode(treeNav, collection.project);
        }
        treeModel.nodeChanged(collection.project);
    }

    public void openCloseProject(boolean isOpen) {
        if (selectedNode != null) {
            TreeCollection collection = selectedNode.getCollection();
            openCloseProject(collection, isOpen);
            reloadCollection(collection);
            collection.save();
        }
    }

    public void reloadTree(TreeNode node) {
        java.awt.EventQueue.invokeLater(() -> {
            treeModel.reload(node);
        });
    }

    public void expandDefaultNodes(TreeCollection collection) {
        expandParendNode(collection.swaggers);
        expandParendNode(collection.testsuites);
        if (collection.testsuites.getChildCount() > 0) {
            expandParendNode((TreeIconNode) collection.testsuites.getChildAt(0));
        }
    }

    public void expandParendNode(TreeIconNode parent) {
        parent.getCollection().expandNode(treeNav, parent);
    }

    public void selectNode(TreeIconNode node) {
        MainPanel.selectedNode = node;
        TreeCollection collection = node.getCollection();
        TreeIconNode debugNode = (node.debugNode != null) ? node.debugNode : collection.project.debugNode;
        editor.reset();
        amphibia.tlbRun.setEnabled(true);
        treeNav.setSelectionPath(new TreePath(node.getPath()));
        debugTreeNav.setSelectionPath(new TreePath(debugNode.getPath()));
        Object raw = node.getCollection().runner.jsonObject();
        switch (node.getType()) {
            case PROJECT:
            case INTERFACE:
                raw = node.getCollection().project.jsonObject();
                break;
            case RULES:
            case REQUEST_ITEM:
            case RESPONSE_ITEM:
            case SCHEMA_ITEM:
                raw = node.jsonObject();
                break;
            case TEST_ITEM:
                raw = node.info.testStepInfo;
                break;
            case TESTSUITE:
            case TESTCASES:
            case TESTS:
            case REQUESTS:
            case SCHEMAS:
                if (node.getParent() != null && ((TreeIconNode) node.getParent()).getType() != TESTCASES) {
                    raw = "";
                }
                break;
            case TESTCASE:
            case TEST_STEP_ITEM:
                final StringBuilder sb = new StringBuilder();
                sb.append("<html><div><b>Request URL:</b> ").append(node.getTreeIconUserObject().getTooltip()).append("</div>");
                sb.append("<div><b>Request Method:</b> ").append(node.jsonObject().getString("method")).append("</div>");
                sb.append("<div><b>Result Status Code:</b> ").append(node.info.testCaseInfo.getJSONObject("properties").get("HTTPStatusCode")).append("</div>");
                sb.append("<br/><b>Request Headers</b><ul>");
                JSONObject testCaseHeaders = JSONObject.fromObject(node.info.testCaseHeaders);
                if (node.info.testCase != null && node.info.testCase.containsKey("headers")) {
                    JSONObject headers = node.info.testCase.getJSONObject("headers");
                    headers.keySet().forEach((key) -> {
                        Object header = headers.get(key);
                        if (header instanceof JSONObject && ((JSONObject) header).isNullObject()) {
                            testCaseHeaders.remove(key);
                        } else {
                            testCaseHeaders.put(key, header);
                        }
                    });
                }
                testCaseHeaders.keySet().forEach((key) -> {
                    sb.append("<li><b>").append(key).append(":</b> ").append(testCaseHeaders.get(key)).append("</li>");
                });
                sb.append("</ul><br/><b>Request Body</b>");
                JSONObject request = node.info.testStepInfo.getJSONObject("request");
                String json = null;
                if (request.get("body") instanceof String) {
                    json = request.getString("body");
                    try {
                        JSONObject properties = request.getJSONObject("properties");
                        json = IO.readFile(node.getCollection(), json);
                        json = IO.prettyJson(json);
                        json = node.info.properties.cloneProperties().setTestStep(properties).replace(json);
                    } catch (Exception ex) {
                        logger.log(Level.SEVERE, null, ex);
                    }
                }
                sb.append("<pre>").append(json).append("</pre>");

                sb.append("<br/><b>Expected Response Body</b>");
                JSONObject response = node.info.testStepInfo.getJSONObject("response");
                json = null;
                if (response.get("body") instanceof String) {
                    json = response.getString("body");
                    try {
                        json = IO.readFile(node.getCollection(), json);
                        json = IO.prettyJson(json);
                        json = node.info.properties.cloneProperties().setTestStep(response.getJSONObject("properties")).replace(json);
                    } catch (Exception ex) {
                        logger.log(Level.SEVERE, null, ex);
                    }
                }
                sb.append("<pre>").append(json).append("</pre>");
                sb.append("</html>");
                raw = sb.toString();
                break;
        }

        try {
            if (raw instanceof JSON) {
                Amphibia.setText(editor.txtRaw, editor.spnRaw, "<pre>" + IO.prettyJson(raw.toString()) + "</pre>");
            } else {
                Amphibia.setText(editor.txtRaw, editor.spnRaw, raw.toString());
            }
        } catch (Exception ex) {
            logger.log(Level.SEVERE, null, ex);
        }

        if (node.getCollection().isOpen()) {
            editor.selectedTreeNode(node);
        }
    }

    public void addError(Exception ex) {
        editor.addError(ex);
    }

    public void setParent(Amphibia amphibia) {
        this.amphibia = amphibia;
        menuBuilder = new TreePopupMenuBuilder(amphibia, this);
    }

    public TreeCollection getSelectedProject(File projectFile) {
        int count = treeModel.getChildCount(treeNode);
        for (int i = 0; i < count; i++) {
            TreeIconNode node = (TreeIconNode) treeModel.getChild(treeNode, i);
            TreeIconNode.TreeIconUserObject useObject = node.getTreeIconUserObject();
            if (projectFile.getAbsolutePath().equals(useObject.getFullPath())) {
                return useObject.getCollection();
            }
        }
        return new TreeCollection();
    }

    public boolean loadProject(TreeCollection collection) {
        boolean success = false;
        try {
            File projectFile = collection.getProjectFile();
            if (!projectFile.exists()) {
                throw new FileNotFoundException(projectFile.getAbsolutePath());
            }
            collection.project.addTooltip(projectFile.getAbsolutePath());

            success = reloadProject(collection);
            if (success && MainPanel.selectedNode == null && collection.project.info != null) {
                MainPanel.selectedNode = collection.project;
            }
        } catch (IOException e) {
            addError(e);
        }
        return success;
    }

    public boolean reloadProject(TreeCollection collection) {
        reset(collection);

        JSONObject projectJson = (JSONObject) IO.getJSON(collection.getProjectFile(), editor);
        collection.project.addJSON(projectJson);
        
        TreeIconNode debugProjectNode = new TreeIconNode(collection.project);
        debugTreeNode.add(debugProjectNode);

        Properties projectProperties;
        try {
            projectProperties = new Properties(projectJson.getJSONArray("globals"), projectJson.getJSONObject("properties"));
        } catch (Exception e) {
            editor.addError(e, Amphibia.getBundle().getString("error_open_json"));
            return false;
        }

        File runnerFile = IO.getFile(collection, "data/runner.json");
        JSONObject json = (JSONObject) IO.getJSON(runnerFile, editor);
        collection.runner.getTreeIconUserObject().update(runnerFile.getAbsolutePath(), json, true);

        treeNode.add(collection.project);
        collection.project.add(collection.swaggers);
        collection.project.add(collection.runner);
        collection.project.add(collection.testsuites);

        String value;
        String pathFormat = "data/tests/%s/%s.json";
        TreeIconNode.TreeIconUserObject userObject = collection.project.getTreeIconUserObject();
        if (!(value = projectJson.getString("name")).equals(userObject.label)) {
            history.renameProject(userObject.label, value, collection);
        }
        collection.setProjectName(value);
        collection.project.getTreeIconUserObject().setLabel(value);

        Map<String, TreeIconNode.ResourceInfo> testcasesMap = new HashMap<>();

        JSONArray projectResources = projectJson.getJSONArray("projectResources");
        JSONArray interfacesJSON = projectJson.getJSONArray("interfaces");
        JSONArray resources = json.getJSONArray("resources");
        JSONArray testsuites = json.getJSONArray("testsuites");
        testsuites.forEach((item) -> {
            JSONObject testsuite = (JSONObject) item;
            int resourceIndex = testsuite.getInt("resources");
            JSONObject resource = projectResources.getJSONObject(resourceIndex);
            JSONObject testSuiteInfo = resource.getJSONObject("testsuites").getJSONObject(testsuite.getString("name"));
            String dirPath = "data/tests/" + testsuite.getString("name");
            File dir = IO.getFile(collection, dirPath);
            for (String name : dir.list()) {
                String path = dirPath + "/" + name;
                File file = new File(dir, name);
                if (file.exists()) {
                    JSONArray testcases = testSuiteInfo.getJSONArray("testcases");
                    for (int i = 0; i < testcases.size(); i++) {
                        JSONObject testCaseInfo = testcases.getJSONObject(i);
                        JSONObject interfaceJSON = interfacesJSON.getJSONObject(resourceIndex);
                        JSONObject testCaseHeaders = JSONObject.fromObject(interfaceJSON.getJSONObject("headers").toString());
                        if (testCaseInfo.containsKey("headers")) {
                            JSONObject headers = testCaseInfo.getJSONObject("headers");
                            headers.keySet().forEach((key) -> {
                                testCaseHeaders.put(key, headers.get(key));
                            });
                        }
                        if (!testcasesMap.containsKey(path) && path.equals(String.format(pathFormat, testsuite.getString("name"), testCaseInfo.getString("name")))) {
                            TreeIconNode.ResourceInfo info = new TreeIconNode.ResourceInfo(file, testsuite, testSuiteInfo, testCaseInfo, testCaseHeaders, (JSONObject) IO.getJSON(file, editor));
                            Properties properties = projectProperties.cloneProperties();
                            properties.setTestSuite(testSuiteInfo.getJSONObject("properties"));
                            properties.setTestCase(JSONObject.fromObject(testCaseInfo.getJSONObject("properties")));
                            info.properties = properties;
                            testcasesMap.put(path, info);
                            break;
                        }
                    }
                }
            }
        });

        for (int i = 0; i < resources.size(); i++) {
            JSONObject item = resources.getJSONObject(i);
            JSONObject interfaceJSON = interfacesJSON.optJSONObject(i);
            boolean isURL = item.getBoolean("isURL");
            TreeIconNode swaggerNode = collection.addTreeNode(collection.swaggers, item.getString("swagger"), SWAGGER, !isURL)
                    .addProperties(DOCUMENT_PROPERTIES)
                    .addJSON(item);
            if (!item.containsKey("states")) {
                item.element("states", new int[]{0});
            }
            swaggerNode.info = new TreeIconNode.ResourceInfo(item.getJSONArray("states"));
            swaggerNode.getTreeIconUserObject().setIsURL(isURL);

            collection.addTreeNode(swaggerNode, interfaceJSON.getString("name"), INTERFACE, false)
                    .addProperties(INTERFACE_PROPERTIES)
                    .addTooltip(interfaceJSON.getString("basePath"))
                    .addJSON(interfaceJSON);

            String props = item.getString("properties");
            if (props != null && !props.isEmpty()) {
                collection.addTreeNode(swaggerNode, props, RULES, true)
                        .addProperties(RULES_PROPERTIES)
                        .addJSON(IO.getJSON(new File(props), editor));
            }
        }
        collection.swaggers.addJSON(resources);

        JSONObject testsuitesJson = new JSONObject();
        for (int i = 0; i < testsuites.size(); i++) {
            JSONObject testsuite = testsuites.getJSONObject(i);
            String name = testsuite.getString("name");
            int resourceIndex = testsuite.getInt("resources");
            JSONObject resource = projectResources.getJSONObject(resourceIndex);

            File dir = IO.getFile(collection, "data/tests/" + Swagger.stripName(name));
            JSONObject testsuiteJSON = new JSONObject();
            testsuiteJSON.put("disabled", testsuite.get("disabled") == Boolean.TRUE);
            testsuiteJSON.put("name", name);
            testsuiteJSON.put("endpoint", "${#Global#" + resource.getString("endpoint") + "}");
            testsuiteJSON.put("interface", resource.getString("interface"));
            testsuiteJSON.put("properties", JSONNull.getInstance());
            testsuiteJSON.put("testcases", JSONNull.getInstance());
            testsuiteJSON.put("index", i);

            JSONObject projectTestSuite = resource.getJSONObject("testsuites").getJSONObject(name);
            IO.replaceValue(testsuiteJSON, "properties", projectTestSuite.get("properties"));

            if (dir.exists()) {
                TreeIconNode testsuiteNode = collection.addTreeNode(collection.testsuites, name, TESTSUITE, false)
                        .addProperties(TESTSUITE_PROPERTIES);
                testsuiteNode.getTreeIconUserObject().setEnabled(testsuite.get("disabled") != Boolean.TRUE);
                testsuiteNode.info = new TreeIconNode.ResourceInfo(dir, testsuite, projectTestSuite, null, null, null);
                testsuiteNode.info.properties = projectProperties;
                if (!testsuite.containsKey("states")) {
                    testsuite.element("states", new int[]{0, 0, 0});
                }
                testsuiteNode.info.states = testsuite.getJSONArray("states");

                TreeIconNode debugSuiteNode = new TreeIconNode(testsuiteNode).addJSON(testsuite);
                debugProjectNode.add(debugSuiteNode);

                JSONArray testcases = new JSONArray();
                for (Iterator it = testsuite.getJSONArray("testcases").iterator(); it.hasNext();) {
                    JSONObject testcase = (JSONObject) it.next();
                    String path = testcase.getString("path");
                    TreeIconNode.ResourceInfo info = testcasesMap.get(path);
                    if (info == null) {
                        logger.log(Level.SEVERE, "The data is undefined: " + path, new NullPointerException(path));
                    } else {
                        testcases.add(testcase.getString("name"));
                        Properties properties = info.properties.cloneProperties();
                        JSONObject testCaseProperties = JSONObject.fromObject(properties.getProperty("TestCase"));
                        JSONObject testCaseHeaders = JSONObject.fromObject(info.testCaseHeaders.toString());

                        if (testcase.containsKey("properties")) {
                            JSONObject props = testcase.getJSONObject("properties");
                            props.keySet().forEach((key) -> {
                                Object prop = props.get(key);
                                if (prop instanceof JSONObject && ((JSONObject) prop).isNullObject()) {
                                    testCaseProperties.remove(key);
                                } else {
                                    testCaseProperties.put(key, prop);
                                }
                            });
                        }

                        if (testcase.containsKey("headers")) {
                            JSONObject headers = testcase.getJSONObject("headers");
                            headers.keySet().forEach((key) -> {
                                Object header = headers.get(key);
                                if (header instanceof JSONObject && ((JSONObject) header).isNullObject()) {
                                    testCaseHeaders.remove(key);
                                } else {
                                    testCaseHeaders.put(key, header);
                                }
                            });
                        }

                        final JSONObject testcaseJSON = new JSONObject();
                        final JSONObject testCaseInheritedProperties = new JSONObject();
                        info.properties.getProperty("Global").keySet().forEach((key) -> {
                            testCaseInheritedProperties.put(key, "${#Global$" + key + "}");
                        });
                        info.properties.getProperty("Project").keySet().forEach((key) -> {
                            testCaseInheritedProperties.put(key, "${#Project$" + key + "}");
                        });
                        info.properties.getProperty("TestSuite").keySet().forEach((key) -> {
                            testCaseInheritedProperties.put(key, "${#TestSuite$" + key + "}");
                        });
                        info.properties.getProperty("TestCase").keySet().forEach((key) -> {
                            testCaseInheritedProperties.put(key, "${#TestCase$" + key + "}");
                        });
                        testCaseProperties.keySet().forEach((key) -> {
                            testCaseInheritedProperties.put(key, "${#TestCase$" + key + "}");
                        });
                        if (testcase.containsKey("transfer")) {
                            JSONObject transferProps = testcase.getJSONObject("transfer");
                            transferProps.keySet().forEach((key) -> {
                                String values;
                                if (testCaseInheritedProperties.containsKey(key)) {
                                    values = testCaseInheritedProperties.get(key) +
                                            ", \n${#TestCase:" + transferProps.get(key) + "}";
                                } else {
                                    values = "${#TestCase:" + transferProps.get(key) + "}";
                                }
                                testCaseInheritedProperties.put(key, values);
                            });
                            testcaseJSON.element("transfer", transferProps);
                        }
                        
                        JSONObject config = info.testCaseInfo.getJSONObject("config");
                        JSONObject replace = config.getJSONObject("replace");
                        String url = "${#Global#" + resource.getString("endpoint") + "}" + "/" + replace.getString("path");
                        
                        testcaseJSON.element("name", testcase.getString("name"));
                        testcaseJSON.element("disabled", testcase.get("disabled") == Boolean.TRUE);
                        testcaseJSON.element("path", info.file.getAbsolutePath());
                        testcaseJSON.element("headers", testCaseHeaders);
                        testcaseJSON.element("properties", testCaseProperties);
                        testcaseJSON.element("method", replace.getString("method"));
                        testcaseJSON.element("url", url);
                        String tooltipURL = properties.replace(url).replaceAll("&amp;", "&");
                        TreeIconNode testcaseNode = collection.addTreeNode(testsuiteNode, testcase.getString("name"), TESTCASE, false)
                                .addProperties(TESTCASE_PROPERTIES)
                                .addTooltip(tooltipURL);
                        testcaseNode.getTreeIconUserObject().setEnabled(testcase.get("disabled") != Boolean.TRUE);
                        testcaseNode.info = info.clone(testcase);
                        if (testcase.containsKey("line")) {
                            testcaseNode.info.consoleLine = testcase.getInt("line");
                        }
                        if (!testcase.containsKey("states")) {
                            testcase.element("states", new int[]{0, 0, 0});
                        }
                        testcaseNode.info.states = testcase.getJSONArray("states");

                        TreeIconNode debugTestCaseNode = new TreeIconNode(testcaseNode).addJSON(testcase);
                        debugSuiteNode.add(debugTestCaseNode);

                        JSONArray teststeps = new JSONArray();
                        JSONObject inheritedProperties = JSONObject.fromObject(testCaseInheritedProperties);
                        testcase.getJSONArray("steps").forEach((item) -> {
                            JSONObject step = (JSONObject) item;
                            teststeps.add(step.getString("name"));
                            JSONObject testStepJSON = JSONObject.fromObject(testcaseNode.info.testStepInfo);
                            testStepJSON.element("path", info.file.getAbsolutePath());
                            testStepJSON.element("method", replace.getString("method"));
                            testStepJSON.element("url", url);
                            step.keySet().forEach((key) -> {
                                HistoryManager.replace(step, testStepJSON);
                            });
                            testStepJSON.element("disabled", step.get("disabled") == Boolean.TRUE);
    
                            JSONObject requestProp = info.testStepInfo.getJSONObject("request").getJSONObject("properties");
                            if (step.containsKey("request")) {
                                requestProp = step.getJSONObject("request").getJSONObject("properties");
                            }
                            
                            JSONObject stepInheritedProperties = JSONObject.fromObject(inheritedProperties);
                            requestProp.keySet().forEach((key) -> {
                                stepInheritedProperties.put(key, "${#TestStep$" + key + "}");
                            });
                            
                            JSONObject responseProp = info.testStepInfo.getJSONObject("response").getJSONObject("properties");
                            if (step.containsKey("response")) {
                                responseProp = step.getJSONObject("response").getJSONObject("properties");
                            }
                            
                            responseProp.keySet().forEach((key) -> {
                                if (!stepInheritedProperties.containsKey(key)) {
                                    stepInheritedProperties.put(key, "${#TestStep$" + key + "}");
                                }
                            });
                            
                            if (step.containsKey("transfer")) {
                                JSONObject transferProps = step.getJSONObject("transfer");
                                transferProps.keySet().forEach((key) -> {
                                    String values;
                                    if (stepInheritedProperties.containsKey(key)) {
                                        values = stepInheritedProperties.get(key) + 
                                                ", \n${#TestStep:" + transferProps.get(key) + "}";
                                    } else {
                                        values = "${#TestStep:" + transferProps.get(key) + "}";
                                    }
                                    stepInheritedProperties.put(key, values);
                                    
                                    if (testCaseInheritedProperties.containsKey(key)) {
                                        values = testCaseInheritedProperties.get(key) + 
                                                ", \n${#TestStep#" + step.getString("name") + ":" + transferProps.get(key) + "}";
                                    } else {
                                        values = "${#TestStep#" + step.getString("name") + ":" + transferProps.get(key) + "}";
                                    }
                                    testCaseInheritedProperties.put(key, values);                                    
                                });
                                testStepJSON.getJSONObject("response").element("transfer", transferProps);
                            }
                            
                            testStepJSON.element("inherited-properties", stepInheritedProperties);
                            
                            String tootltip = info.properties.cloneProperties().setTestStep(requestProp).replace(url).replaceAll("&amp;", "&");
                            TreeIconNode testStepNode = collection.insertTreeNode(testcaseNode, step.getString("name"), TEST_STEP_ITEM)
                                    .addProperties(TEST_STEP_ITEM_PROPERTIES)
                                    .addTooltip(tootltip)
                                    .addJSON(testStepJSON);
                            testStepNode.getTreeIconUserObject().setEnabled(step.get("disabled") != Boolean.TRUE);
                            testStepNode.info = info.clone(testcase, step);
                            if (step.containsKey("line")) {
                                testStepNode.info.consoleLine = step.getInt("line");
                            }
                            if (!step.containsKey("states")) {
                                step.element("states", new int[]{0, 0, 0});
                            }
                            testStepNode.info.states = step.getJSONArray("states");

                            debugTestCaseNode.add(new TreeIconNode(testStepNode, null).addJSON(step));
                        });
                        testcaseJSON.element("teststeps", teststeps);
                        testcaseJSON.element("inherited-properties", testCaseInheritedProperties);
                        testcaseNode.addJSON(testcaseJSON);
                    }
                }
                IO.replaceValue(testsuiteJSON, "testcases", testcases);
                testsuiteNode.addJSON(testsuiteJSON);
            }

            testsuitesJson.element(name, dir.getAbsolutePath());
        }

        collection.testsuites
                .addProperties(VIEW_ITEM_PROPERTIES)
                .addJSON(testsuitesJson);

        JSONObject testSuites = new JSONObject();
        projectResources.forEach((resource) -> {
            JSONObject resourseJSON = (JSONObject) resource;
            JSONObject testsuiteList = resourseJSON.getJSONObject("testsuites");
            testsuiteList.keySet().forEach((name) -> {
                JSONObject testSuiteItem = testsuiteList.getJSONObject(name.toString());
                JSONObject testSuiteJSON = new JSONObject();
                File file = IO.getFile(collection, "data/tests/" + name);
                testSuites.put(name, file.getAbsolutePath());
                testSuiteJSON.element("name", name);
                testSuiteJSON.element("endpoint", resourseJSON.getString("endpoint"));
                testSuiteJSON.element("interface", resourseJSON.getString("interface"));

                testSuiteJSON.element("properties", JSONNull.getInstance());
                IO.replaceValue(testSuiteJSON, "properties", testSuiteItem.getJSONObject("properties"));

                TreeIconNode node = collection.addTreeNode(collection.tests, name.toString(), TESTSUITE, false)
                        .addProperties(TEST_TESTSUITE_PROPERTIES)
                        .addJSON(testSuiteJSON);
                node.info = new TreeIconNode.ResourceInfo(file);

                JSONArray testcases = new JSONArray();
                testSuiteItem.getJSONArray("testcases").forEach((testcase) -> {
                    JSONObject testCase = (JSONObject) testcase;
                    testcases.add(testCase.getString("name"));

                    JSONObject testcaseJSON = new JSONObject();
                    JSONObject config = testCase.getJSONObject("config");
                    JSONObject replace = config.getJSONObject("replace");
                    testcaseJSON.element("name", testCase.getString("name"));
                    testcaseJSON.element("type", testCase.getString("type"));
                    testcaseJSON.element("summary", testCase.getString("summary"));
                    testcaseJSON.element("operationId", config.getString("operationId"));
                    testcaseJSON.element("method", replace.getString("method"));
                    testcaseJSON.element("path", replace.getString("path"));
                    testcaseJSON.element("example", replace.get("body"));
                    testcaseJSON.element("headers", JSONNull.getInstance());
                    testcaseJSON.element("properties", JSONNull.getInstance());

                    IO.replaceValue(testcaseJSON, "properties", testCase.getJSONObject("properties"));
                    IO.replaceValue(testcaseJSON, "headers", testCase.getJSONObject("headers"));

                    String path = String.format(pathFormat, name, testCase.getString("name"));
                    TreeIconNode.ResourceInfo info = testcasesMap.get(path);

                    TreeIconNode testcaseNode = collection.addTreeNode(node, testCase.getString("name"), TESTCASE, false)
                            .addProperties(TEST_TESTCASE_PROPERTIES)
                            .addTooltip(replace.getString("path"))
                            .addJSON(testcaseJSON);
                    testcaseNode.info = info;

                    if (info != null) {
                        JSONObject testStepJSON = JSONObject.fromObject(testcaseNode.info.testStepInfo);
                        testStepJSON.element("path", info.file.getAbsolutePath());
                        testcaseNode.info.testStepInfo.keySet().forEach((key) -> {
                            testStepJSON.put(key, testcaseNode.info.testStepInfo.get(key));
                        });
                        TreeIconNode childNode = collection.addTreeNode(testcaseNode, info.file.getName(), null, false)
                                .addTooltip(info.file.getAbsolutePath())
                                .addProperties((Object[][]) TEST_ITEM_PROPERTIES)
                                .addJSON(testStepJSON)
                                .addType(TEST_ITEM);
                        childNode.info = testcaseNode.info;
                    }
                });
                testSuiteJSON.element("testcases", testcases);
            });
        });
        collection.tests.addProperties((Object[][]) VIEW_ITEM_PROPERTIES)
                .addJSON(testSuites);
        if (Amphibia.isExpertView()) {
            collection.project.add(collection.tests);
        }

        Object[][] loadNodes = new Object[][]{
            {"requests", collection.requests, REQUEST_ITEM, VIEW_ITEM_PROPERTIES, VIEW_ITEM_PROPERTIES},
            {"responses", collection.responses, RESPONSE_ITEM, VIEW_ITEM_PROPERTIES, VIEW_ITEM_PROPERTIES},
            {"schemas", collection.schemas, SCHEMA_ITEM, VIEW_ITEM_PROPERTIES, EDIT_ITEM_PROPERTIES}
        };

        for (Object[] item : loadNodes) {
            File suiteDir = IO.getFile(collection, "data/" + item[0].toString());
            TreeIconNode parentNode = (TreeIconNode) item[1];
            TreeCollection.TYPE type = (TreeCollection.TYPE) item[2];
            if (Amphibia.isExpertView()) {
                collection.project.add(parentNode);
            }
            if (suiteDir.exists()) {
                JSONObject itemJSON = new JSONObject();
                for (String dir : suiteDir.list()) {
                    File subdir = new File(suiteDir, dir);
                    TreeIconNode node;
                    if (parentNode != collection.schemas) {
                        node = collection.addTreeNode(parentNode, dir, TESTSUITE, false);
                    } else {
                        node = collection.addTreeNode(parentNode, dir, null, false).addType(type);
                    }
                    node.info = new TreeIconNode.ResourceInfo(subdir);
                    node.addProperties(RESOURCES_PROPERTIES)
                            .addTooltip(subdir.getAbsolutePath())
                            .addJSON(new JSONObject().element("path", subdir.getAbsolutePath()).element("files", subdir.list()));
                    itemJSON.element(dir, subdir.getAbsolutePath());
                    for (String name : subdir.list()) {
                        File file = new File(suiteDir, dir + "/" + name);
                        if (file.isFile()) {
                            TreeIconNode childNode = collection.addTreeNode(node, name, null, false)
                                    .addTooltip(file.getAbsolutePath())
                                    .addProperties((Object[][]) item[4])
                                    .addJSON(IO.getJSON(file, editor))
                                    .addType(type);
                            childNode.info = new TreeIconNode.ResourceInfo(file);
                        }
                    }
                }
                parentNode.addProperties((Object[][]) item[3]).addJSON(itemJSON);
            }
        }

        Enumeration children = debugTreeNode.children();
        while (children.hasMoreElements()) {
            TreeIconNode node = (TreeIconNode) children.nextElement();
            if (node.getChildCount() == 0) {
                node.removeFromParent();
            }
        }
 
        amphibia.enableSave(true);
        treeModel.reload();
        debugTreeModel.reload();

        amphibia.mnuOpen.setEnabled(!collection.isOpen());
        amphibia.mnuClose.setEnabled(collection.isOpen());

        //Update user preferences
        String projects = userPreferences.get(Amphibia.P_RECENT_PROJECTS, "[]");
        try {
            JSONArray list = (JSONArray) IO.toJSON(projects);
            JSONObject item = new JSONObject();
            item.element("uuid", collection.getUUID());
            item.element("name", collection.getProjectName());
            item.element("path", collection.getProjectFile().getCanonicalPath());
            for (int i = list.size() - 1; i >= 0; i--) {
                JSONObject o = list.getJSONObject(i);
                if (o.getString("name").equals(item.getString("name"))
                        && o.getString("path").equals(item.getString("path"))) {
                    list.remove(o);
                    break;
                }
            }
            list.add(0, item);
            amphibia.createRecentProjectMenu(list);
            userPreferences.put(Amphibia.P_RECENT_PROJECTS, list.toString());
        } catch (Exception ex) {
            addError(ex);
        }

        JSONObject states;
        if (!json.containsKey("states")) {
            states = new JSONObject();
            states.element("project", new int[]{1,1,0});
            states.element("swaggers", new int[]{0});
            states.element("testsuites", new int[]{1});
            states.element("tests", new int[]{0});
            states.element("schemas", new int[]{0});
            states.element("requests", new int[]{0});
            states.element("responses", new int[]{0});
            json.element("states", states);
        } else {
            states = json.getJSONObject("states");
        }
        collection.project.info = new TreeIconNode.ResourceInfo(states.getJSONArray("project"));
        collection.swaggers.info = new TreeIconNode.ResourceInfo(states.getJSONArray("swaggers"));
        collection.testsuites.info = new TreeIconNode.ResourceInfo(states.getJSONArray("testsuites"));
        collection.tests.info = new TreeIconNode.ResourceInfo(states.getJSONArray("tests"));
        collection.schemas.info = new TreeIconNode.ResourceInfo(states.getJSONArray("schemas"));
        collection.requests.info = new TreeIconNode.ResourceInfo(states.getJSONArray("requests"));
        collection.responses.info = new TreeIconNode.ResourceInfo(states.getJSONArray("responses"));
        
        debugProjectNode.info = collection.project.info;

        if (!json.containsKey("expandResources")) {
            json.element("expandResources", new JSONObject());
        }

        IO.write(collection.runner, editor);
        return true;
    }

    public void reloadAll() {
        List<TreeIconNode> nodes = new ArrayList<>();
        for (int i = 0; i < treeModel.getChildCount(treeNode); i++) {
            TreeIconNode node = (TreeIconNode) treeModel.getChild(treeNode, i);
            nodes.add(node);
        }
        treeNode.removeAllChildren();
        nodes.forEach((node) -> {
            reloadCollection(node.getCollection());
        });
    }

    public void reloadCollection(TreeCollection collection) {
        reloadProject(collection);
        refreshCollection(collection);
    }

    public void refreshCollection(TreeCollection collection) {
        if (collection.isOpen()) {
            try {
                JSONObject expandResources = collection.runner.jsonObject().getJSONObject("expandResources");
                Enumeration children = collection.project.preorderEnumeration();
                while (children.hasMoreElements()) {
                    TreeIconNode node = (TreeIconNode) children.nextElement();
                    if (node.info != null && treeNav.isExpanded(new TreePath(((TreeIconNode) node.getParent()).getPath()))) {
                        if (node.info.states != null) {
                            if (node.info.states.getInt(TreeIconNode.STATE_PROJECT_EXPAND) == 1) {
                                treeNav.expandPath(new TreePath(node.getPath()));
                            }
                        } else if (expandResources.containsKey(node.info.file.getAbsolutePath())) {
                            treeNav.expandPath(new TreePath(node.getPath()));
                        }
                    }
                }
                children = collection.project.debugNode.preorderEnumeration();
                while (children.hasMoreElements()) {
                    TreeIconNode node = (TreeIconNode) children.nextElement();
                    if (node.info != null && node.info.states != null && node.info.states.getInt(TreeIconNode.STATE_DEBUG_EXPAND) == 1) {
                        if (node.getParent() != null && debugTreeNav.isExpanded(new TreePath(((TreeIconNode) node.getParent()).getPath()))) {
                            debugTreeNav.expandPath(new TreePath(node.getPath()));
                        }
                    }
                }
                if (selectedNode != null) {
                    selectNode(selectedNode);
                }
            } catch (Exception e) {
                logger.log(Level.SEVERE, e.toString(), e);
            }
        } else {
            openCloseProject(collection, false);
        }
    }

    public boolean undo() {
        return editor.undo();
    }

    public boolean redo() {
        return editor.redo();
    }

    private boolean isNodeEnabled(TreeNode node) {
        Object userObject = ((TreeIconNode) node).getUserObject();
        if (userObject instanceof TreeIconNode.TreeIconUserObject) {
            TreeIconNode.TreeIconUserObject c = (TreeIconNode.TreeIconUserObject) userObject;
            if (c.json instanceof JSONObject) {
                JSONObject json = (JSONObject) c.json;
                if (json.containsKey("disabled") && json.getBoolean("disabled")) {
                    return false;
                }
            }
            if (node.getParent() != null) {
                return isNodeEnabled(node.getParent());
            }
        }
        return true;
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        spnMainPane = new JSplitPane();
        tabLeft = new JTabbedPane();
        spnTreeNav = new JScrollPane();
        treeNav = new JTree();
        spnDebugTreeNav = new JScrollPane();
        debugTreeNav = new JTree();
        spnReport = new JScrollPane();
        reportTreeNav = new JTree();
        tabRight = new JTabbedPane();
        editor = new Editor();
        wizard = new Wizard();

        setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));

        spnMainPane.setDividerLocation(320);
        spnMainPane.setDividerSize(3);

        treeNav.setModel(treeModel);
        spnTreeNav.setViewportView(treeNav);

        ResourceBundle bundle = ResourceBundle.getBundle("com/equinix/amphibia/messages"); // NOI18N
        tabLeft.addTab(bundle.getString("projects"), new ImageIcon(getClass().getResource("/com/equinix/amphibia/icons/project_16.png")), spnTreeNav); // NOI18N

        debugTreeNav.setModel(this.debugTreeModel);
        spnDebugTreeNav.setViewportView(debugTreeNav);

        tabLeft.addTab(bundle.getString("debugger"), new ImageIcon(getClass().getResource("/com/equinix/amphibia/icons/debugger_16.png")), spnDebugTreeNav); // NOI18N

        reportTreeNav.setModel(reportTreeModel);
        spnReport.setViewportView(reportTreeNav);

        tabLeft.addTab(bundle.getString("report"), new ImageIcon(getClass().getResource("/com/equinix/amphibia/icons/export_junit_16.png")), spnReport); // NOI18N

        spnMainPane.setLeftComponent(tabLeft);

        tabRight.addTab(bundle.getString("editor"), new ImageIcon(getClass().getResource("/com/equinix/amphibia/icons/editor_16.png")), editor); // NOI18N
        tabRight.addTab(bundle.getString("wizard"), new ImageIcon(getClass().getResource("/com/equinix/amphibia/icons/wizard_16.png")), wizard); // NOI18N

        spnMainPane.setRightComponent(tabRight);

        add(spnMainPane);
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    JTree debugTreeNav;
    public Editor editor;
    private JTree reportTreeNav;
    private JScrollPane spnDebugTreeNav;
    private JSplitPane spnMainPane;
    private JScrollPane spnReport;
    JScrollPane spnTreeNav;
    public JTabbedPane tabLeft;
    public JTabbedPane tabRight;
    JTree treeNav;
    Wizard wizard;
    // End of variables declaration//GEN-END:variables
}
