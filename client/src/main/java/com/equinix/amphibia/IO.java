/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.equinix.amphibia;

import com.equinix.amphibia.components.BaseTaskPane;
import com.equinix.amphibia.components.MainPanel;
import com.equinix.amphibia.components.TreeCollection;
import com.equinix.amphibia.components.TreeIconNode;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONNull;
import net.sf.json.JSONObject;
import org.apache.commons.collections.map.ListOrderedMap;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

/**
 *
 * @author dgofman
 */
public class IO {
    
    public static final JSONNull NULL = JSONNull.getInstance();
    
    public static JSONArray toJSONArray(Object obj) {
        return JSONArray.fromObject(obj);
    }
    
    public static JSONObject toJSONObject(Object obj) {
        return JSONObject.fromObject(obj);
    }

    public static boolean isNULL(Object value) {
        if (value == null) {
            return true;
        } else if (value == NULL) {
            return true;
        } else if (value instanceof JSONObject) {
            return ((JSONObject) value).isNullObject();
        }
        return false;
    }

    public static JSON toJSON(String json) throws Exception {
        if (json.startsWith("[")) {
            return IO.toJSONArray(json);
        } else {
            return IO.toJSONObject(json);
        }
    }

    public static JSON getJSON(File file) throws Exception {
        JSON json = null;
        try {
            FileInputStream fis = new FileInputStream(file);
            json = (JSON) toJSON(IOUtils.toString(fis));
            fis.close();
        } catch (Exception ex) {
            throw new Exception("File: " + file.getAbsolutePath() + "\n" + ex.toString(), ex);
        }
        return json;
    }
    
    public static JSONObject getBackupJSON(File file, BaseTaskPane pane) {
        File backupFile = getBackupFile(file);
        if (!backupFile.exists()) {
            backupFile = file;
        }
        return (JSONObject) getJSON(backupFile, pane);
    }
    
    public static File getBackupFile(File file) {
        return new File(file.getParentFile(), FilenameUtils.getBaseName(file.getName()) + ".bak");
    }

    public static String prettyJson(String value) throws Exception {
        ScriptEngine scriptEngine = new ScriptEngineManager().getEngineByName("JavaScript");
        scriptEngine.put("jsonString", value);
        scriptEngine.eval("result = JSON.stringify(JSON.parse(jsonString), null, 4)");
        String json = ((String) scriptEngine.get("result")).replaceAll(" {4}", "\t");
        return json == null || "null".equals(json) ? "" : json;
    }

    public static String readFile(File file, BaseTaskPane pane) {
        try {
            return readFile(file);
        } catch (IOException ex) {
            pane.addError(ex);
            return null;
        }
    }

    public static String readFile(File file) throws IOException {
        FileInputStream fis = new FileInputStream(file);
        String str = IOUtils.toString(fis);
        fis.close();
        return str;
    }
    
    public static String readFile(TreeCollection collection, String filePath) throws IOException {
        return readFile(new File(collection.getProjectDir(), filePath));
    }
    
    public static String readFile(TreeCollection collection, String filePath, BaseTaskPane pane) {
        return readFile(new File(collection.getProjectDir(), filePath), pane);
    }

    public static JSON getJSON(TreeCollection collection, String filePath, BaseTaskPane pane) {
        return getJSON(new File(collection.getProjectDir(), filePath), pane);
    }

    public static JSON getJSON(File file, BaseTaskPane pane) {
        try {
            return getJSON(file);
        } catch (Exception ex) {
            pane.addError(file, ex);
            return null;
        }
    }

    public static JSON getJSON(TreeCollection collection, String filePath) throws Exception {
        return getJSON(getFile(collection, filePath));
    }

    public static File getFile(TreeCollection collection, String path) {
        return new File(collection.getProjectDir(), path);
    }
    
    public static File getFile(String path) {
        File file = new File(path);
        if (file.exists()) {
            return file;
        } else {
            return getFile(MainPanel.selectedNode.getCollection(), path);
        }
    }

    public static String[] write(TreeIconNode node, BaseTaskPane pane) {
        try {
            return write(node);
        } catch (Exception ex) {
            pane.addError(ex);
        }
        return new String[] {};
    }

    public static String[] write(TreeIconNode node) throws Exception {
        TreeIconNode.TreeIconUserObject userObject = node.getTreeIconUserObject();
        File file = new File(userObject.getFullPath());
        File backup = getBackupFile(file);
        if (backup.exists()) {
            file = backup;
        }
        return write(userObject.json.toString(), file, true);
    }
    
    public static String[] write(String content, File file, boolean isJSON) throws Exception {
        if (isJSON) {
            content = prettyJson(content);
        }

        if (!file.exists()) {
            file.createNewFile();
        }
        String oldContent = readFile(file);
        write(content, file);
        return new String[] { oldContent, content };
    }
    
    public static void write(String content, File file) throws IOException {
        IOUtils.write(content, new FileOutputStream(file));
    }

    public static void copy(File source, File target) throws IOException {
        IOUtils.copy(new FileInputStream(source), new FileOutputStream(target));
    }
    
    public static void copyDir(File source, File target) throws IOException {
        FileUtils.copyDirectory(source, target);
    }
    
    public static void replaceValue(JSONObject source, String key, Object value) {
        Set<ListOrderedMap.Entry> entries = source.entrySet();
        entries.forEach((Map.Entry entry) -> {
            ListOrderedMap.Entry mapEntry = (ListOrderedMap.Entry) entry;
            if (key.equals(entry.getKey())) {
                entry.setValue(value);
            }
        });
    }
    
    public static void replaceValues(JSONObject source, JSONObject target) {
        Set<ListOrderedMap.Entry> entries = source.entrySet();
        entries.forEach((Map.Entry entry) -> {
            target.put(entry.getKey(), entry.getValue());
        });
    }
}
