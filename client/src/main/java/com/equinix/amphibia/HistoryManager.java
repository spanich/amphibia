/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.equinix.amphibia;

import static com.equinix.amphibia.Amphibia.getUserPreferences;
import static com.equinix.amphibia.components.TreeCollection.TYPE.*;

import com.equinix.amphibia.components.Editor;
import com.equinix.amphibia.components.MainPanel;
import com.equinix.amphibia.components.TreeCollection;
import com.equinix.amphibia.components.TreeIconNode;
import java.util.Enumeration;

import java.util.logging.Logger;
import java.util.prefs.Preferences;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

/**
 *
 * @author dgofman
 */
public class HistoryManager {
    
    private MainPanel mainPanel;
    private Editor editor;
    
    private final Preferences userPreferences = getUserPreferences();
    
    private static final Logger logger = Logger.getLogger(HistoryManager.class.getName());
    
    public HistoryManager(MainPanel mainPanel, Editor editor) {
        this.mainPanel = mainPanel;
        this.editor = editor;
    }
    
    public void saveEntry(Editor.Entry entry, TreeCollection collection) {
        TreeIconNode selectedNode = MainPanel.selectedNode;
        TreeIconNode.ResourceInfo info = selectedNode.info;
        TreeCollection.TYPE type = selectedNode.getType();
        TreeIconNode node = (TreeIconNode.ProfileNode) collection.profile;
        if (type == PROJECT || type == INTERFACE) {
            node = collection.project;
        }
        if ("name".equals(entry.name)) {
            TreeIconNode saveNode = collection.profile;
            if (type == TESTCASE) {
                info.testCase.element(entry.name, entry.value);
            } else if (type == TEST_STEP_ITEM) {
                info.testStep.element("name", entry.value);
            } else {
                JSONObject json = selectedNode.jsonObject();
                if (type == PROJECT) {
                    json = saveNode.jsonObject().getJSONObject("project");
                } else if (type == INTERFACE) {
                    saveNode = collection.project;
                }
                json.element(entry.name, entry.value);
            }
            saveAndAddHistory(saveNode);
            //Save new selection name
            selectedNode.getTreeIconUserObject().setLabel(entry.value.toString());
            selectedNode.saveSelection();
            mainPanel.reloadCollection(collection);
        } else if ("disabled".equals(entry.name)) {
            JSONObject json;
            switch (type) {
                case TESTSUITE:
                    JSONArray testsuites = node.jsonObject().getJSONArray("testsuites");
                    int index = selectedNode.jsonObject().getInt("index");
                    json = testsuites.getJSONObject(index);
                    break;
                case TESTCASE:
                    json = info.testCase;
                    break;
                case TEST_STEP_ITEM:
                    json = info.testStep;
                    break;
                default:
                    return;
            }
            if (entry.value != Boolean.TRUE) {
                json.remove("disabled");
            } else {
                json.element("disabled", true);
            }
        } else if (type == TESTSUITE) {
            if ("properties".equals(entry.getParent().toString())) {
                updateValues(entry, info.testSuiteInfo.getJSONObject("properties"), info.testSuite, "properties");
            }
        } else if (type == TESTCASE) {
            if ("summary".equals(entry.name)) {
                info.testCaseInfo.element(entry.name, entry.value);
            } else if ("operationId".equals(entry.name)) {
                info.testCaseInfo.getJSONObject("config").element(entry.name, entry.value);
            } else if ("method".equals(entry.name) || "path".equals(entry.name) || "example".equals(entry.name)) {
                info.testCaseInfo.getJSONObject("config").getJSONObject("replace").element("example".equals(entry.name) ? "body" : entry.name, entry.value);
            } else if ("properties".equals(entry.getParent().toString())) {
                if (info.testCase != null) { //update profile.json
                    updateValues(entry, info.testCaseInfo.getJSONObject("properties"), info.testCase, "properties");
                }
            } else if ("headers".equals(entry.getParent().toString())) {
                if (info.testCase != null) { //update profile.json
                    updateValues(entry, info.testCaseHeaders, info.testCase, "headers");
                }
            } else {
                return;
            }
        } else if (type == RULES || type == TEST_ITEM || type == SCHEMA_ITEM) {
            node = selectedNode;
        } else if (type == TEST_STEP_ITEM) {
            JSONObject json = selectedNode.jsonObject();
            info.testStep.remove("request");
            info.testStep.remove("response");
            JSONObject request = compare(info.testStepInfo.getJSONObject("request"), json.getJSONObject("request"), false);
            if (!request.isEmpty()) {
                info.testStep.element("request", request);
            }
            JSONObject response = compare(info.testStepInfo.getJSONObject("response"), json.getJSONObject("response"), false);
            if (!response.isEmpty()) {
                info.testStep.element("response", response);
            }
        }
        saveNodeValue(node);
    }
    
    public static JSONObject compare(JSONObject source, JSONObject target, boolean isProperties) {
        source.keySet().forEach((key) -> {
            if (isProperties || !"properties".equals(key)) {
                if (String.valueOf(source.get(key)).equals(String.valueOf(target.get(key)))) {
                    target.remove(key);
                }
            }
        });
        if (!isProperties) {
            JSONObject properties = target.getJSONObject("properties");
            compare(source.getJSONObject("properties"), properties, true);
            if (properties.isEmpty()) {
                target.remove("properties");
            }
        }
        return target;
    }
    
    public static void replace(JSONObject source, JSONObject target) {
        if (target == null || target.isNullObject()) {
            return;
        }
        source.keySet().forEach((k) -> {
            String key = k.toString();
            if (source.get(key) instanceof JSONObject && !source.getJSONObject(key).isNullObject()) {
                if (target.getJSONObject(key).isNullObject()) {
                    target.element(key, source.get(key));
                } else {
                    replace(source.getJSONObject(key), target.getJSONObject(key));
                }
            } else {
                target.element(key, source.get(key));
            }
        });
    }

    private void updateValues(Editor.Entry entry, JSONObject source, JSONObject target, String name) {
        JSONObject json = target.getJSONObject(name);
        if (json.isNullObject()) {
            json = new JSONObject();
        }
        if (entry.isDelete) {
            json.remove(entry.name);
        } else if (source.containsKey(entry.name) && String.valueOf(source.get(entry.name)).equals(String.valueOf(entry.value))) {
            json.remove(entry.name);
        } else {
            json.element(entry.name, entry.value);
        }
        if (json.isEmpty()) {
            target.remove(name);
        } else {
            target.element(name, json);
        }
    }
    
    public void saveNodeValue(TreeIconNode node) {
        mainPanel.history.saveAndAddHistory(node);
        mainPanel.reloadCollection(node.getCollection());
    }

    public void resetHistory(boolean all) {
        editor.resetHistory();
    }

    public void renameResource() {
        int index = 0;
        String[] names = new String[MainPanel.selectedNode.getParent().getChildCount() - 1];
        Enumeration children = MainPanel.selectedNode.getParent().children();
        while (children.hasMoreElements()) {
            Object node = children.nextElement();
            if (node != MainPanel.selectedNode) {
                names[index++] = node.toString();
            }
        }
        String name = Amphibia.instance.inputDialog("renameResources", MainPanel.selectedNode.getLabel(), names);
        if (name != null && !name.isEmpty()) {
            Editor.Entry entry = new Editor.Entry("name");
            entry.value = name;
            saveEntry(entry, MainPanel.selectedNode.getCollection());
        }
    }

    public boolean addHistory(String oldContent, String newContent, TreeIconNode node) {
        return addHistory(node.getTreeIconUserObject().getFullPath(), oldContent, newContent);
    }

    public boolean addHistory(String filePath, String oldContent, String newContent) {
        return editor.addHistory(null, filePath, oldContent, newContent);
    }

    public void saveAndAddHistory(TreeIconNode node) {
        String[] contents = IO.write(node, editor);
        addHistory(contents[0], contents[1], node);
    }
}