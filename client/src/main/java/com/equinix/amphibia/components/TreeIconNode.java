/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.equinix.amphibia.components;

import static com.equinix.amphibia.Amphibia.getUserPreferences;

import com.equinix.amphibia.Amphibia;

import java.io.File;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;
import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import com.equinix.amphibia.agent.builder.Properties;

/**
 *
 * @author dgofman
 */
@SuppressWarnings("NonPublicExported")
public final class TreeIconNode extends DefaultMutableTreeNode {

    private final TreeIconUserObject nodeUserObject;

    private static final Logger logger = Logger.getLogger(TreeIconNode.class.getName());
    private static final Preferences userPreferences = getUserPreferences();
    private static String selectedUIDName;

    public static final Icon runIcon;
    public static final Icon runningIcon;
    public static final Icon passedIcon;
    public static final Icon failedIcon;
    public static final Icon skippedIcon;

    public ResourceInfo info;
    public TreeIconNode source;
    public TreeIconNode debugNode;

    public static final int STATE_PROJECT_EXPAND = 0;
    public static final int STATE_DEBUG_EXPAND = 1;
    public static final int STATE_DEBUG_REPORT = 2;

    public static final int REPORT_INIT_STATE = 0;
    public static final int REPORT_RUN_STATE = 1;
    public static final int REPORT_RUNNING_STATE = 2;
    public static final int REPORT_PASSED_STATE = 3;
    public static final int REPORT_SKIPPED_STATE = 4;
    public static final int REPORT_ERROR_STATE = 5;
    public static final int REPORT_FAILED_STATE = 6;

    static {
        byte[] data = userPreferences.getByteArray(Amphibia.P_SELECTED_NODE, null);
        if (data != null) {
            setSelectedUIDName(new String(data));
        }
        runIcon = new ImageIcon(TreeIconNode.class.getResource("/com/equinix/amphibia/icons/testcase_run_16.png"));
        runningIcon = new ImageIcon(TreeIconNode.class.getResource("/com/equinix/amphibia/icons/testcase_running_16.png"));
        passedIcon = new ImageIcon(TreeIconNode.class.getResource("/com/equinix/amphibia/icons/testcase_passed_16.png"));
        failedIcon = new ImageIcon(TreeIconNode.class.getResource("/com/equinix/amphibia/icons/testcase_failed_16.png"));
        skippedIcon = new ImageIcon(TreeIconNode.class.getResource("/com/equinix/amphibia/icons/testcase_skipped_16.png"));
    }

    public TreeIconNode() {
        this(null, "root", null, false);
        this.addType(TreeCollection.TYPE.ROOT);
    }

    public TreeIconNode(TreeCollection collection, String label, TreeCollection.TYPE type, boolean truncate) {
        this(collection, label, type, truncate, null);
    }

    public TreeIconNode(TreeCollection collection, String label, TreeCollection.TYPE type, boolean truncate, Object[][] properties) {
        this(new TreeIconUserObject(collection, label, type, truncate, properties));
    }

    public TreeIconNode(TreeIconNode source, TreeCollection.TYPE type) {
        this(source.getCollection(), source.getLabel(), type, false);
        this.source = source;
        this.info = source.info;
        this.addType(source.getType());
        this.nodeUserObject.setEnabled(source.nodeUserObject.isEnabled());
        source.debugNode = this;
    }

    public TreeIconNode(TreeIconNode source) {
        this(source, source.getType());
    }

    public TreeIconNode addProperties(Object[][] properties) {
        nodeUserObject.properties = properties;
        return this;
    }

    public TreeIconNode addJSON(JSON json) {
        nodeUserObject.json = json;
        return this;
    }
    
    public TreeIconNode addType(TreeCollection.TYPE type) {
        nodeUserObject.type = type;
        return this;
    }

    public JSONObject jsonObject() {
        return (JSONObject) nodeUserObject.json;
    }

    public JSONArray jsonArray() {
        return (JSONArray) nodeUserObject.json;
    }

    public TreeCollection.TYPE getType() {
        return nodeUserObject.type;
    }

    public TreeIconNode addTooltip(String tooltip) {
        nodeUserObject.setTooltip(tooltip);
        return this;
    }

    public String getLabel() {
        return nodeUserObject.getLabel();
    }

    public TreeIconNode(TreeIconUserObject nodeUserObject) {
        super(nodeUserObject);
        this.nodeUserObject = nodeUserObject;
    }

    public TreeCollection getCollection() {
        return getTreeIconUserObject().getCollection();
    }

    public TreeIconNode setReportState(int state) {
        if (nodeUserObject.json instanceof JSONObject && jsonObject().containsKey("states")) {
            jsonObject().getJSONArray("states").set(STATE_DEBUG_REPORT, state);
        }
        return this;
    }

    public int getReportState() {
        if (nodeUserObject.json instanceof JSONObject && jsonObject().containsKey("states")) {
            return jsonObject().getJSONArray("states").getInt(STATE_DEBUG_REPORT);
        }
        return REPORT_INIT_STATE;
    }

    public Icon getReportIcon() {
        switch (getReportState()) {
            case REPORT_RUN_STATE:
                return runIcon;
            case REPORT_RUNNING_STATE:
                return runningIcon;
            case REPORT_PASSED_STATE:
                return passedIcon;
            case REPORT_ERROR_STATE:
            case REPORT_FAILED_STATE:
                return failedIcon;
            case REPORT_SKIPPED_STATE:
                return skippedIcon;
            default:
                return null;
        }
    }

    @Override
    public void setParent(MutableTreeNode newParent) {
        super.setParent(newParent);
        String path = getNodePath();
        if (getUID(path).equals(selectedUIDName)) {
            MainPanel.setSelectedNode(this);
        }
    }

    public String getNodePath() {
        return Arrays.toString(this.getPath());
    }
    
    public void saveSelection() {
        setSelectedUIDName(getUID(getNodePath()));
        userPreferences.putByteArray(Amphibia.P_SELECTED_NODE, selectedUIDName.getBytes());
    }

    public String getUID(String path) {
        return getCollection().getUUID() + "_" + path;
    }

    public TreeIconUserObject getTreeIconUserObject() {
        return nodeUserObject;
    }

    public TreeIconNode cloneNode() {
        TreeIconNode node = (TreeIconNode) clone();
        node.source = this;
        return node;
    }

    public TreeIconNode getSource() {
        return source;
    }
    
    private static void setSelectedUIDName(String name) {
        selectedUIDName = name;
    }

    static public class ResourceInfo {

        public File file;
        public JSONObject resource; //resource item in projectResources array
        public JSONObject testSuite; //(profile.json) get and change testcases order
        public JSONObject testCase; //(profile.json) get and set testcase name and disabled status
        public JSONObject testStep; //(profile.json) get diff of parent test file
        public JSONObject testSuiteInfo; //get testsuite properties
        public JSONObject testCaseInfo; //get testcase properties and configiration
        public JSONObject testCaseHeaders; //combination interface and testcase headers
        public JSONObject testStepInfo; //teststep file data
        public JSONArray states; //states of node, indexes: (0 - project expand state, 1 - debug expand state, 2 - run report state)
        public Properties properties;
        public int consoleLine;

        public ResourceInfo(File file) {
            this.file = file;
        }

        public ResourceInfo(JSONArray states) {
            this.states = states;
        }

        public ResourceInfo(File file, JSONObject resource, JSONObject testSuite, JSONObject testSuiteInfo, JSONObject testCaseInfo, JSONObject testCaseHeaders, JSONObject testStepInfo) {
            this.file = file;
            this.resource = resource;
            this.testSuite = testSuite;
            this.testSuiteInfo = testSuiteInfo;
            this.testCaseInfo = testCaseInfo;
            this.testCaseHeaders = testCaseHeaders;
            this.testStepInfo = testStepInfo;
            this.testStep = null;
        }

        public ResourceInfo clone(JSONObject testCase) {
            ResourceInfo clone = new ResourceInfo(file, resource, testSuite, testSuiteInfo, testCaseInfo, testCaseHeaders, testStepInfo);
            clone.testCase = testCase;
            clone.properties = properties;
            return clone;
        }

        public ResourceInfo clone(JSONObject testCase, JSONObject testStep) {
            ResourceInfo clone = clone(testCase);
            clone.testStep = testStep;
            return clone;
        }
    }

    static public class TreeIconUserObject {

        protected TreeCollection collection;
        protected String label;
        protected String fullPath;
        protected TreeCollection.TYPE type;
        protected Icon icon;
        protected boolean enabled;
        protected Object[][] properties;

        public JSON json;

        @SuppressWarnings("OverridableMethodCallInConstructor")
        public TreeIconUserObject(TreeCollection collection, String label, TreeCollection.TYPE type, boolean truncate, Object[][] properties) {
            this.collection = collection;
            this.type = type;
            this.enabled = true;
            this.type = type;
            this.properties = properties;
            this.setIcon(type);
            update(label, null, truncate);
        }

        public void setLabel(String value) {
            label = value;
        }

        public String getLabel() {
            return label;
        }

        public String getFullPath() {
            return fullPath;
        }

        public String getTooltip() {
            return fullPath;
        }

        public void setTooltip(String value) {
            fullPath = value;
        }

        public TreeCollection.TYPE getType() {
            return type;
        }

        public void setType(TreeCollection.TYPE value) {
            type = value;
        }

        public Icon getIcon() {
            return icon;
        }

        protected void setIcon(Object iconType) {
            if (iconType != null) {
                setIcon(iconType.toString().toLowerCase() + "_16.png");
            } else {
                icon = null;
            }
        }

        protected void setIcon(String iconName) {
            try {
                icon = new ImageIcon(TreeIconNode.class.getResource("/com/equinix/amphibia/icons/" + iconName));
            } catch (Exception e) {
                logger.log(Level.SEVERE, iconName, e);
            }
        }

        public TreeCollection getCollection() {
            return collection;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public void update(String label, String fullPath, JSONObject json) {
            this.label = label;
            this.fullPath = fullPath;
            this.json = json;
        }

        public void update(String label, JSONObject json, boolean truncate) {
            this.update(truncate ? new File(label).getName() : label, label, json);
        }

        @Override
        public String toString() {
            return label;
        }
    }
}
