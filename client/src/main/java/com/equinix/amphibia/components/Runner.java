/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.equinix.amphibia.components;

import com.equinix.amphibia.Amphibia;
import com.equinix.amphibia.IO;
import java.awt.Color;
import java.awt.Desktop;

import java.awt.EventQueue;
import java.awt.Rectangle;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.net.HttpURLConnection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.Target;
import org.apache.tools.ant.taskdefs.optional.junit.AggregateTransformer;
import org.apache.tools.ant.taskdefs.optional.junit.XMLResultAggregator;
import org.apache.tools.ant.types.FileSet;

/**
 *
 * @author dgofman
 */
public final class Runner extends BaseTaskPane {

    private boolean isRunning;
    private boolean includeSkippedTests;
    private boolean autoSwitchConsole;
    private Editor editor;
    private Thread currentThread;
    private TreeIconNode selectedNode;
    private DefaultTreeModel treeModel;
    private HttpURLConnection conn;
    private Map<Thread, Boolean> threads;
    private boolean continueOnError;
    private int lineIndex;

    private final SimpleDateFormat reportDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

    public static int DEFAULT_TIMEOUT = 60;
    public static Color GREEN = new Color(40, 130, 10);

    public Runner(MainPanel mainPanel, Editor editor) {
        super();
        this.editor = editor;
        this.mainPanel = mainPanel;
        this.tree = mainPanel.debugTreeNav;
        this.treeModel = mainPanel.debugTreeModel;
        this.threads = new HashMap<>();
    }

    @Override
    public DefaultMutableTreeNode addWarning(String warning) {
        return editor.addToTree(editor.warnings, warning);
    }

    @Override
    public DefaultMutableTreeNode addError(String error) {
        return editor.addToTree(editor.errors, error);
    }

    @SuppressWarnings("SleepWhileInLoop")
    public Runner addToConsole(String text, Color color, boolean isBold) {
        if (text != null && !text.isEmpty()) {
            StyledDocument doc = editor.txtConsole.getStyledDocument();
            SimpleAttributeSet attr = new SimpleAttributeSet();
            StyleConstants.setForeground(attr, color);
            StyleConstants.setBold(attr, isBold);

            if (autoSwitchConsole) {
                editor.activateTab(Amphibia.TAB_CONSOLE, Amphibia.instance.mnuConsole);
            }

            try {
                lineIndex = getContextHeight();
                doc.insertString(doc.getLength(), text, attr);
                if (text.split("\n").length > 1) {
                    int newLineIndex = getContextHeight();
                    do {
                        lineIndex = newLineIndex;
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException ex) {
                        }
                        newLineIndex = getContextHeight();
                    } while (lineIndex != newLineIndex);
                }
            } catch (BadLocationException ex) {
                logger.log(Level.SEVERE, null, ex);
            }
        }
        return this;
    }

    private int getContextHeight() {
        try {
            return editor.txtConsole.getUI().getMinimumSize(this).height;
        } catch (Exception e) {
            return lineIndex;
        }
    }

    public Runner date() {
        return addToConsole("\n" + dateMediumFormat.print(new Date().getTime()) + "\n\n", Color.black, true);
    }

    public Runner info(String text) {
        return info(text, false);
    }

    public Runner info(String text, boolean isBool) {
        return addToConsole(text, Color.BLUE, isBool);
    }

    public Runner fine(String text) {
        return fine(text, false);
    }

    public Runner fine(String text, boolean isBool) {
        return addToConsole(text, GREEN, isBool);
    }

    public Runner error(String text) {
        return error(text, false);
    }

    public Runner error(String text, boolean isBool) {
        return addToConsole(text, Color.RED, isBool);
    }

    public void runTests() {
        TreeIconNode node = MainPanel.selectedNode;
        if (node == null) {
            return;
        }
        resetConsole();
        threads.clear();
        isRunning = true;
        mainPanel.reportTreeNode.removeAllChildren();
        mainPanel.reportTreeModel.reload();

        autoSwitchConsole = userPreferences.getBoolean(Amphibia.P_SWITCH_CONSOLE, true);
        includeSkippedTests = userPreferences.getBoolean(Amphibia.P_SKIPPED_TEST, true);
        continueOnError = userPreferences.getBoolean(Amphibia.P_CONTINUE_ON_ERROR, true);

        TreeIconNode projectNode = node.getCollection().project.debugNode;

        //Reset all nodes
        Enumeration children = projectNode.preorderEnumeration();
        while (children.hasMoreElements()) {
            TreeIconNode child = (TreeIconNode) children.nextElement();
            child.setReportState(TreeIconNode.REPORT_INIT_STATE);
            if (child.getType() == TreeCollection.TYPE.TEST_STEP_ITEM || child.getType() == TreeCollection.TYPE.TESTCASE) {
                child.jsonObject().remove("error");
                child.jsonObject().remove("time");
                child.jsonObject().remove("timestamp");
            }
        }

        if (tree.getSelectionPath() != null) {
            selectedNode = (TreeIconNode) tree.getSelectionPath().getLastPathComponent();
            //Set run state
            selectedNode.setReportState(TreeIconNode.REPORT_RUN_STATE);
            children = selectedNode.preorderEnumeration();
            while (children.hasMoreElements()) {
                TreeIconNode child = (TreeIconNode) children.nextElement();
                child.setReportState(TreeIconNode.REPORT_RUN_STATE);
                tree.expandPath(new TreePath(child.getPath()));
                treeModel.nodeStructureChanged(child);
            }

            startThread();
        }
    }

    public void stopTests() {
        if (selectedNode == null) {
            return;
        }
        if (conn != null) {
            conn.disconnect();
            conn = null;
        }
        threads.clear();

        Enumeration children = selectedNode.preorderEnumeration();
        while (children.hasMoreElements()) {
            TreeIconNode child = (TreeIconNode) children.nextElement();
            if (child.getReportState() == TreeIconNode.REPORT_RUN_STATE) {
                child.setReportState(TreeIconNode.REPORT_SKIPPED_STATE);
            }
        }
        IO.write(MainPanel.selectedNode.getCollection().runner, Runner.this);

        selectedNode = null;
        isRunning = false;
        Amphibia.instance.btnPause.setSelected(false);
    }

    public void resetConsole() {
        lineIndex = 0;
        editor.txtConsole.setText("");
        if (MainPanel.selectedNode != null) {
            TreeIconNode runner = MainPanel.selectedNode.getCollection().runner;
            JSONArray testsuites = runner.jsonObject().getJSONArray("testsuites");
            testsuites.forEach((testsuite) -> {
                JSONArray testcases = ((JSONObject) testsuite).getJSONArray("testcases");
                testcases.forEach((item) -> {
                    JSONObject testcase = (JSONObject) item;
                    testcase.remove("line");
                    testcase.getJSONArray("steps").forEach((step) -> {
                        ((JSONObject) step).remove("line");
                    });
                });
            });
            mainPanel.saveNodeValue(runner);
        }
    }

    public void pauseResumeTests(boolean isPaused) {
        if (selectedNode == null) {
            Amphibia.instance.btnPause.setSelected(false);
            return;
        }
        threads.put(currentThread, false);
        isRunning = !isPaused;
        if (isRunning) {
            startThread();
        }
    }

    public void generateJUnitReport() throws IOException {
        if (MainPanel.selectedNode == null) {
            return;
        }
        TreeCollection collection = MainPanel.selectedNode.getCollection();
        TreeIconNode runner = collection.runner;
        JSONArray testsuites = runner.jsonObject().getJSONArray("testsuites");
        File dir = IO.getFile(collection, "reports");
        if (!dir.exists()) {
            dir.mkdirs();
        }

        PrintWriter pw = new PrintWriter(new FileOutputStream(new File(dir, "junit-noframes.xml"), false));
        pw.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        pw.println("<testsuites>");

        for (int i = 0; i < testsuites.size(); i++) {
            final int index = i;
            final double[] data = {
                0, //0 - time
                0, //1 - tests
                0, //2 - skipped
                0, //3 - errors
                0 //4 - failures (asserts)
            };
            JSONObject testsuite = testsuites.getJSONObject(index);
            if (testsuite.containsKey("timestamp")) {
                JSONArray testcases = testsuite.getJSONArray("testcases");
                StringBuilder sb = new StringBuilder();
                String testSuiteName = testsuite.getString("name");
                testcases.forEach((tc) -> {
                    JSONObject testcase = (JSONObject) tc;
                    String classname = testcase.getString("name");
                    if (testcase.containsKey("steps") && testcase.getJSONArray("steps").size() > 0) {
                        JSONArray teststeps = testcase.getJSONArray("steps");
                        teststeps.forEach((ts) -> {
                            JSONObject step = (JSONObject) ts;
                            sb.append(addTestCaseInfo(data, testSuiteName, classname, step.getString("name"), step));
                        });
                    } else {
                         sb.append(addTestCaseInfo(data, testSuiteName, classname, "", testcase));
                    }
                });
                
                StringBuilder sb2 = new StringBuilder();
                sb2.append("\t<testsuite time=\"")
                        .append(data[0]).append("\" tests=\"")
                        .append(data[1]).append("\" skipped=\"")
                        .append(data[2]).append("\" errors=\"")
                        .append(data[3]).append("\" failures=\"")
                        .append(data[4]).append("\" id=\"")
                        .append(index).append("\" name=\"")
                        .append(testSuiteName)
                        .append("\" package=\"")
                        .append(collection.getProjectName())
                        .append("\" timestamp=\"")
                        .append(testsuite.getString("timestamp"))
                        .append("\">\n");
                sb2.append(sb.toString());
                sb2.append("\t</testsuite>");
                
                pw.println(sb2.toString());
                
                File xmlFile = new File(dir, "TEST-" + testsuite.get("name") + ".xml");
                PrintWriter xmlPw = new PrintWriter(new FileOutputStream(xmlFile, false));
                xmlPw.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
                xmlPw.println(sb2.toString());
                xmlPw.close();
            }
        }
        pw.println("</testsuites>");
        pw.close();

        System.setProperty("PROJECT_NAME", collection.getProjectName());
        
        Project project = new Project();
        project.init();

        Target target = new Target();
        target.setName("junitreport");
        project.addTarget(target);

        FileSet fs = new FileSet();
        fs.setDir(dir);
        fs.createInclude().setName("TEST-*.xml");
        
        AggregateTransformer.Format format = new AggregateTransformer.Format();
        format.setValue(AggregateTransformer.NOFRAMES);

        XMLResultAggregator aggregator = new XMLResultAggregator();
        aggregator.addFileSet(fs);
        aggregator.setProject(project);
        aggregator.setTodir(dir);

        AggregateTransformer transformer = aggregator.createReport();
        transformer.setFormat(format);
        transformer.setStyledir(new File("resources"));
        transformer.setTodir(dir);
        
        target.addTask(aggregator);
        project.executeTarget("junitreport");

        Desktop desktop = Desktop.getDesktop();
        desktop.open(dir);
    }

    private String addTestCaseInfo(double[] data, String testSuiteName, String className, String name, JSONObject result) {
        if (result.containsKey("states")) {
            String info = "\n\t\t\t";
           
            switch (result.getJSONArray("states").getInt(2)) {
                case TreeIconNode.REPORT_PASSED_STATE:
                    info = "";
                    break;
                case TreeIconNode.REPORT_SKIPPED_STATE:
                    info += "<skipped message=\"\"/>";
                    data[2]++;
                    break;
                case TreeIconNode.REPORT_ERROR_STATE:
                    JSONArray err = result.getJSONArray("error");
                    info += "<error message=\"" + StringEscapeUtils.escapeXml(err.getString(0)) + "\" type=\"" + err.get(1) + "\"><![CDATA[" + err.get(2) + "\n\n" + err.get(3) + "]]></error>";
                    data[3]++;
                    break;
                case TreeIconNode.REPORT_FAILED_STATE:
                    JSONArray fail = result.getJSONArray("error");
                    info += "<failure message=\"" +  StringEscapeUtils.escapeXml(fail.getString(0)) + "\" type=\"" + fail.get(1) + "\"><![CDATA[" + fail.get(2) + "\n\n" + fail.get(3) + "]]></failure>";
                    data[4]++;
                    break;
                default:
                    return "";
            }
            
            double time = result.containsKey("time") ? result.getDouble("time") / 1000 : 0;
            data[0] += time;
            data[1]++;
            
            return "\t\t<testcase parent=\"" + testSuiteName + "\" classname=\"" + className + "\" name=\"" + name + "\" time=\"" + time + "\">" + info + "\n\t\t</testcase>\n";
        }
        return "";
    }
    
     public void openReport() {
        resetConsole();
        if (MainPanel.selectedNode == null) {
            return;
        }
        mainPanel.reportTreeNode.removeAllChildren();
        TreeCollection collection = MainPanel.selectedNode.getCollection();
        TreeIconNode runner = collection.runner;
        JSONArray testsuites = runner.jsonObject().getJSONArray("testsuites");
        testsuites.forEach((ts) -> {
            JSONArray testcases = ((JSONObject)ts).getJSONArray("testcases");
            testcases.forEach((tc) -> {
                JSONObject testcase = (JSONObject) tc;
                boolean addTascase = testcase.containsKey("time");
                TreeIconNode testCaseNode = new TreeIconNode(collection, testcase.getString("name"), TreeCollection.TYPE.TESTCASE, false).addJSON(testcase);
                if (testcase.containsKey("error")) {
                    List<String> err = new ArrayList<>();
                    testcase.getJSONArray("error").forEach((val) -> {
                        err.add(String.valueOf(val));
                    });
                    testCaseNode.add(new TreeIconNode(collection, String.join("\n", err), null, false).addType(TreeCollection.TYPE.ERRORS));
                }
                JSONArray steps = testcase.getJSONArray("steps");
                for (int i = 0; i < steps.size(); i++) {
                    JSONObject step = steps.getJSONObject(i);
                    if (step.containsKey("time")) {
                        addTascase = true;
                        TreeIconNode testStepNode = new TreeIconNode(collection, step.getString("name"), null, false)
                                        .addType(TreeCollection.TYPE.TEST_STEP_ITEM)
                                        .addJSON(step);
                        testCaseNode.add(testStepNode);
                        if (step.containsKey("error")) {
                            List<String> err = new ArrayList<>();
                            step.getJSONArray("error").forEach((val) -> {
                                err.add(String.valueOf(val));
                            });
                            testStepNode.add(new TreeIconNode(collection, String.join("\n", err), null, false).addType(TreeCollection.TYPE.ERRORS));
                        }
                    }
                }
                if (addTascase) {
                    mainPanel.reportTreeNode.add(testCaseNode);
                }
            });
        });
        mainPanel.reportTreeModel.reload();
     }

    public boolean isRunning() {
        return isRunning;
    }

    private boolean isRunning(Thread thread) {
        if (!threads.containsKey(thread)) {
            return false;
        }
        return threads.get(thread);
    }

    private void startThread() {
        currentThread = new Thread() {
            @Override
            public void run() {
                Thread thread = currentThread;
                executeTest(thread, null, selectedNode, selectedNode.getReportState());
                if (isRunning(thread)) {
                    stopTests();
                    mainPanel.reloadAll();
                }
            }
        };
        threads.put(currentThread, true);
        currentThread.start();
    }

    private synchronized int executeTest(Thread thread, TreeIconNode parent, TreeIconNode node, int resultState) {
        EventQueue.invokeLater(() -> {
            Rectangle rect = tree.getPathBounds(new TreePath(node.getPath()));
            if (rect != null) {
                rect.x = 0;
                try {
                    tree.scrollRectToVisible(rect);
                } catch (Exception e) {
                }
            }
        });

        if (node.getType() == TreeCollection.TYPE.PROJECT) {
            resultState = TreeIconNode.REPORT_RUN_STATE;
        }

        if (node.getType() == TreeCollection.TYPE.TESTSUITE) {
            node.jsonObject().element("timestamp", reportDateFormat.format(new Date()));
        }

        if (node.source != null && node.source.getTreeIconUserObject().json != null) {
            if (node.source.jsonObject().getOrDefault("disabled", false) == Boolean.TRUE) {
                resultState = TreeIconNode.REPORT_SKIPPED_STATE;
            }
        }

        Enumeration children = node.children();
        if (isRunning(thread) && !children.hasMoreElements()
                && (node.getType() == TreeCollection.TYPE.TEST_STEP_ITEM || node.getType() == TreeCollection.TYPE.TESTCASE)) {
            TreeIconNode parentNode = parent;
            if (parentNode == null) {
                parentNode = mainPanel.reportTreeNode;
            }
            if (includeSkippedTests && resultState == TreeIconNode.REPORT_SKIPPED_STATE) {
                node.jsonObject().element("time", 0);
                parentNode.add(node.cloneNode());
            } else if (resultState == TreeIconNode.REPORT_RUN_STATE) {
                node.setReportState(TreeIconNode.REPORT_RUNNING_STATE);
                treeModel.nodeStructureChanged(node);
                JSONObject json = node.jsonObject();
                Result result = new Result();
                long startTime = new Date().getTime();
                int startIndex = lineIndex;
                try {
                    date();
                    result = request(node.source);
                } catch (Exception e) {
                    result.addError(node.source.jsonObject(), e);
                }
                node.info.consoleLine = Math.max(0, startIndex - 16);
                result.time = new Date().getTime() - startTime;
                json.element("time", result.time);
                json.element("line", node.info.consoleLine);

                info("STATUS: ", true).info(result.statusCode + "\n");
                info("TIME: ", true).info(result.time + " ms\n");
                info("RESULT:\n", true);
                final String content = (result.content != null ? result.content + "\n" : "");
                if (result.exception == null) {
                    resultState = TreeIconNode.REPORT_PASSED_STATE;
                    parentNode.add(node.cloneNode().addJSON(json));
                    fine(content + "\n");
                } else {
                    String[] error = result.createError();
                    json.element("error", error);
                    resultState = TreeIconNode.REPORT_ERROR_STATE;
                    error(content + "\n").error("ERROR:\n", true).error(error[2] + "\n");
                    TreeIconNode child = node.cloneNode().addJSON(json);
                    parentNode.add(child);
                    child.add(new TreeIconNode(child.getCollection(), String.join("\n", error), null, false).addType(TreeCollection.TYPE.ERRORS));
                    if (!continueOnError) {
                        stopTests();
                    }
                }
            }
            EventQueue.invokeLater(() -> {
                mainPanel.reportTreeModel.reload(mainPanel.reportTreeNode);
            });
        } else {
            final TreeIconNode parentNode = (node.getType() == TreeCollection.TYPE.TESTCASE) ? node.cloneNode() : null;
            if (parentNode != null) {
                EventQueue.invokeLater(() -> {
                    mainPanel.reportTreeNode.add(parentNode);
                });
            }
            while (isRunning(thread) && children.hasMoreElements()) {
                TreeIconNode child = (TreeIconNode) children.nextElement();
                int childResultState = resultState == TreeIconNode.REPORT_SKIPPED_STATE ? resultState : child.getReportState();
                childResultState = executeTest(thread, parentNode, child, childResultState);
                if (resultState == TreeIconNode.REPORT_RUN_STATE && childResultState == TreeIconNode.REPORT_ERROR_STATE) {
                    resultState = childResultState;
                }
                if (parentNode != null) {
                    parentNode.setReportState(resultState);
                    EventQueue.invokeLater(() -> {
                        mainPanel.reportTreeModel.nodeChanged(parentNode);
                    });
                }
            }
            if (resultState == TreeIconNode.REPORT_RUN_STATE) {
                resultState = TreeIconNode.REPORT_PASSED_STATE;
            }
            if (!isRunning(thread)) {
                return resultState;
            }
        }

        node.setReportState(resultState);
        EventQueue.invokeLater(() -> {
            treeModel.nodeStructureChanged(node);
        });
        return resultState;
    }

    private Result request(TreeIconNode node) throws Exception {
        conn = null;
        Result result = new Result();
        BufferedReader in;
        JSONObject json = node.jsonObject();
        info("NAME: ", true).info(json.getString("name") + "\n");
        try {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            String url = node.getTreeIconUserObject().getTooltip();
            info(json.getString("method") + ": ", true).info(url + "\n");
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setConnectTimeout(userPreferences.getInt(Amphibia.P_CONN_TIMEOUT, DEFAULT_TIMEOUT) * 1000);
            conn.setReadTimeout(userPreferences.getInt(Amphibia.P_READ_TIMEOUT, DEFAULT_TIMEOUT) * 1000);

            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setRequestMethod(json.getString("method"));
            conn.setRequestProperty("Accept", "*/*");

            final JSONObject testCaseHeaders = JSONObject.fromObject(node.info.testCaseHeaders);
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

            info("HEADER:\n", true);
            testCaseHeaders.keySet().forEach((key) -> {
                info(key + ": " + testCaseHeaders.get(key) + "\n");
                conn.setRequestProperty(key.toString().toLowerCase(), String.valueOf(testCaseHeaders.get(key)));
            });

            JSONObject request = node.info.testStepInfo.getJSONObject("request");
            String body = null;
            if (request.get("body") instanceof String) {
                body = request.getString("body");
                try {
                    body = IO.readFile(node.getCollection(), body);
                    body = IO.prettyJson(body);
                    body = node.info.properties.cloneProperties().setTestStep(request.getJSONObject("properties")).replace(body);
                } catch (Exception ex) {
                    logger.log(Level.SEVERE, null, ex);
                }
            }
            info("BODY:\n", true).info(body + "\n");

            if (body != null && !body.isEmpty()) {
                conn.getOutputStream().write(body.getBytes("UTF-8"));
            }

            InputStream content;
            try {
                content = (InputStream) conn.getInputStream();
            } catch (IOException e) {
                if (conn == null) { //User pressed stop button
                    result.content = "Connection aborted";
                    result.addError(json, e);
                    return result;
                }
                result.addError(json, e);
                content = (InputStream) conn.getErrorStream();
            }
            if (content != null) {
                in = new BufferedReader(new InputStreamReader(content));
                String line;
                while ((line = in.readLine()) != null) {
                    pw.println(line);
                }
                in.close();

                try {
                    result.content = IO.prettyJson(sw.toString());
                } catch (Exception e) {
                    result.content = sw.toString();
                }
            }
        } catch (IOException e) {
            result.addError(json, e);
        } finally {
            if (conn != null) {
                try {
                    result.statusCode = conn.getResponseCode();
                    int expected = node.info.testCaseInfo.getJSONObject("properties").getInt("HTTPStatusCode");
                    if (result.statusCode != expected) {
                        throw new AssertionError("StatusCode expected " + result.statusCode + " to equal " + expected);
                    }
                } catch (AssertionError | IOException e) {
                    result.addError(json, e);
                }
                result.headers = conn.getHeaderFields();
                conn.disconnect();
            }
        }
        conn = null;
        return result;
    }

    class Result {

        Throwable exception = null;
        Map<String, List<String>> headers;
        String content;
        int statusCode;
        long time;

        public void addError(JSONObject json, Throwable t) {
            if (exception == null) {
                exception = t;
                String message = json.getString("name") + " (" + t.getMessage() + ")";
                if (t instanceof java.net.UnknownHostException || t instanceof java.net.SocketTimeoutException) {
                    editor.addError(message);
                } else {
                    editor.addError(t, message);
                }
            }
        }

        public String[] createError() {
            return new String[]{exception.getMessage(), 
                    exception.getClass().getName(), 
                    StringUtils.join(exception.getStackTrace(), "\n\t", 0, 5), content != null ? content : ""};
        }
    }
}