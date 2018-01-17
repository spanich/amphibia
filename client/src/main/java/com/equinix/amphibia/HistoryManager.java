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

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import net.sf.json.JSONArray;
import net.sf.json.JSONNull;
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

    public void renameProject(String oldName, String newName, TreeCollection collection) {
        String projects = userPreferences.get(Amphibia.P_RECENT_PROJECTS, "[]");
        try {
            JSONArray list = (JSONArray) IO.toJSON(projects);
            for (int i = 0; i < list.size(); i++) {
                if (collection.getUUID().equals(list.getJSONObject(i).getString("uuid"))) {
                    list.remove(i);
                    userPreferences.put(Amphibia.P_RECENT_PROJECTS, list.toString());
                    break;
                }
            }
            mainPanel.selectNode(collection.project);
        } catch (Exception ex) {
            logger.log(Level.SEVERE, null, ex);
        }
    }
    
    public void saveEntry(Editor.Entry entry, TreeCollection collection) {
        TreeIconNode node = collection.project;
        TreeIconNode.ResourceInfo info = MainPanel.selectedNode.info;
        TreeCollection.TYPE type = MainPanel.selectedNode.getType();
        if ("name".equals(entry.name)) {
            MainPanel.selectedNode.getTreeIconUserObject().setLabel((String)entry.value);
            MainPanel.selectedNode.saveSelection();    
        }
        if ("disabled".equals(entry.name)) {
            JSONObject json;
            switch (type) {
                case TESTSUITE:
                    node = collection.profile;
                    JSONArray testsuites = node.jsonObject().getJSONArray("testsuites");
                    int index = MainPanel.selectedNode.jsonObject().getInt("index");
                    json = testsuites.getJSONObject(index);
                    break;
                case TESTCASE:
                    json = info.testCase;
                    node = collection.profile;
                    break;
                case TEST_STEP_ITEM:
                    json = info.testStep;
                    node = collection.profile;
                    break;
                default:
                    return;
            }
            if (entry.value != Boolean.TRUE) {
                json.remove("disabled");
            } else {
                json.element("disabled", true);
            }
        } else if (type == TESTCASE) {
            if ("name".equals(entry.name)) {
                info.testCase.element(entry.name, entry.value);
                node = collection.profile;
            } else if ("summary".equals(entry.name)) {
                info.testCaseInfo.element(entry.name, entry.value);
            } else if ("operationId".equals(entry.name)) {
                info.testCaseInfo.getJSONObject("config").element(entry.name, entry.value);
            } else if ("method".equals(entry.name) || "path".equals(entry.name) || "example".equals(entry.name)) {
                info.testCaseInfo.getJSONObject("config").getJSONObject("replace").element("example".equals(entry.name) ? "body" : entry.name, entry.value);
            } else if ("properties".equals(entry.getParent().toString())) {
                if (info.testCase != null) { //update profile.json
                    node = collection.profile;
                    updateValues(entry, info.testCaseInfo.getJSONObject("properties"), info.testCase, "properties");
                }
            } else if ("headers".equals(entry.getParent().toString())) {
                if (info.testCase != null) { //update profile.json
                    updateValues(entry, info.testCaseHeaders, info.testCase, "headers");
                    node = collection.profile;
                }
            } else {
                return;
            }
        } else if (type == PROFILE) {
            node = collection.profile;
        } else if (type == RULES || type == TEST_ITEM || type == SCHEMA_ITEM) {
            node = MainPanel.selectedNode;
        } else if (type == TEST_STEP_ITEM) {
            JSONObject json = MainPanel.selectedNode.jsonObject();
            info.testStep.remove("request");
            info.testStep.remove("response");

            info.testStep.element("name", json.getString("name"));
            JSONObject request = compare(info.testStepInfo.getJSONObject("request"), json.getJSONObject("request"), false);
            if (!request.isEmpty()) {
                info.testStep.element("request", request);
            }
            JSONObject response = compare(info.testStepInfo.getJSONObject("response"), json.getJSONObject("response"), false);
            if (!response.isEmpty()) {
                info.testStep.element("response", response);
            }
            node = collection.profile;
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

    private void updateValues(Editor.Entry entry, JSONObject source, JSONObject testCase, String name) {
        JSONObject json = testCase.getJSONObject(name);
        if (json.isNullObject()) {
            json = new JSONObject();
        }
        if (entry.isDelete) {
            if (source.containsKey(entry.name)) {
                json.element(entry.name, JSONNull.getInstance());
            } else {
                json.remove(entry.name);
            }
        } else if (source.containsKey(entry.name) && String.valueOf(source.get(entry.name)).equals(String.valueOf(entry.value))) {
            json.remove(entry.name);
        } else {
            json.element(entry.name, entry.value);
        }
        if (json.isEmpty()) {
            testCase.remove(name);
        } else {
            testCase.element(name, json);
        }
    }
    
    public void saveNodeValue(TreeIconNode node) {
        mainPanel.history.saveAndAddHistory(node);
        mainPanel.reloadCollection(node.getCollection());
    }

    public void resetHistory(boolean all) {
        editor.resetHistory();
    }

    public void renameResource(boolean isProject, TreeCollection collection) {
        TreeIconNode.TreeIconUserObject userObject = MainPanel.selectedNode.getTreeIconUserObject();
        String name = Amphibia.instance.inputDialog("renameResources", userObject.getLabel(), new String[] {});
        if (name != null && !name.isEmpty()) {
            if (isProject) {
                renameProject(userObject.getLabel(), name, collection);
            }
            ((JSONObject) userObject.json).element("name", name);
            saveAndAddHistory(collection.project);
            mainPanel.reloadCollection(collection);
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