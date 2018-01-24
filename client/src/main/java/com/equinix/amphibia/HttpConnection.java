/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.equinix.amphibia;

import com.equinix.amphibia.agent.builder.Properties;
import com.equinix.amphibia.components.TreeCollection;
import com.equinix.amphibia.components.TreeIconNode;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import net.sf.json.JSONObject;

/**
 *
 * @author dgofman
 */
public final class HttpConnection {
    
    private Preferences userPreferences;
    private HttpURLConnection conn;
    private IHttpConnection out;
    
    public static int DEFAULT_TIMEOUT = 60;
    private static final Logger logger = Logger.getLogger(HttpConnection.class.getName());
    
    public HttpConnection(IHttpConnection out) {
        this.out = out;
        userPreferences = Amphibia.getUserPreferences();
    }
    
    @SuppressWarnings("NonPublicExported")
    public Result request(String name, String method, TreeIconNode node) throws Exception {
        final TreeCollection collection = node.getCollection();
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
        JSONObject request = node.info.testStepInfo.getJSONObject("request");
        String reqBody = null;
        if (request.get("body") instanceof String) {
            reqBody = request.getString("body");
            try {
                reqBody = IO.readFile(collection, reqBody);
                reqBody = IO.prettyJson(reqBody);
                reqBody = node.info.properties.cloneProperties().setTestStep(request.getJSONObject("properties")).replace(reqBody);
            } catch (Exception ex) {
                logger.log(Level.SEVERE, null, ex);
            }
        }
        
        Result result = request(collection.getProjectProperties(), name, method, node.getTreeIconUserObject().getTooltip(), testCaseHeaders, reqBody);
        try {
            int expected = node.info.testCaseInfo.getJSONObject("properties").getInt("HTTPStatusCode");
            if (result.statusCode != expected) {
                throw new AssertionError("StatusCode expected " + result.statusCode + " to equal " + expected);
            }
        } catch (AssertionError e) {
            addError(result, name, e);
        }
        
        return result;
    }
    
    @SuppressWarnings("NonPublicExported")
    public Result request(Properties properties, String name, String method, String url, JSONObject headers, String reqBody) throws Exception {
        conn = null;
        Result result = new Result();
        BufferedReader in;
        long startTime = new Date().getTime();

        out.info("NAME: ", true).info(name + "\n");
        try {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            out.info(method + ": ", true).info(url + "\n");
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setConnectTimeout(userPreferences.getInt(Amphibia.P_CONN_TIMEOUT, DEFAULT_TIMEOUT) * 1000);
            conn.setReadTimeout(userPreferences.getInt(Amphibia.P_READ_TIMEOUT, DEFAULT_TIMEOUT) * 1000);

            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setRequestMethod(method);
            conn.setRequestProperty("Accept", "*/*");

            out.info("HEADER:\n", true);
            if (conn != null) {
                headers.keySet().forEach((key) -> {
                    String value = properties.replace(headers.get(key));
                    out.info(key + ": " + value + "\n");
                    conn.setRequestProperty(key.toString().toLowerCase(), value);
                });
            }

            out.info("BODY:\n", true).info(reqBody + "\n");

            if (conn != null && reqBody != null && !reqBody.isEmpty()) {
                conn.getOutputStream().write(reqBody.getBytes("UTF-8"));
            }

            InputStream content;
            try {
                if (conn == null) { //User pressed stop button
                    addError(result, name, new SocketTimeoutException(result.content = "Connection aborted"));
                    return result;
                }
                content = (InputStream) conn.getInputStream();
            } catch (IOException e) {
                addError(result, name, e);
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
            addError(result, name, e);
        } finally {
            result.time = new Date().getTime() - startTime;
            if (conn != null) {
                try {
                    result.statusCode = conn.getResponseCode();
                } catch (IOException e) {
                    throw e;
                }
                result.headers = conn.getHeaderFields();
                conn.disconnect();
            }
        }
        conn = null;
        return result;
    }
    
    public HttpURLConnection urlConnection() {
        return conn;
    }
    
    public void disconnect() {
        if (conn != null) {
            conn.disconnect();
        }
        conn = null;
    }
    
    public void addError(Result result, String name, Throwable t) {
        if (result.exception == null) {
            result.exception = t;
            String message = name + " (" + t.getMessage() + ")";
            if (t instanceof java.net.UnknownHostException || t instanceof SocketTimeoutException) {
                out.addError(message);
            } else {
                out.addError(t, message);
            }
        }
    }

    public static class Result {

        public Throwable exception = null;
        public Map<String, List<String>> headers;
        public String content;
        public int statusCode;
        public long time;

        public String[] createError() {
            List<String> sb = new ArrayList<>();
            StackTraceElement[] stack = exception.getStackTrace();
            for (int i = 0; i < stack.length; i++) {
                String line = stack[i].toString();
                if (line.startsWith("com.equinix.amphibia")) {
                    if (i >= 4) {
                        sb.add(line);
                        break;
                    }
                }
                if (i < 4) {
                    sb.add(line);
                }
            }
            return new String[]{exception.getMessage(), 
                    exception.getClass().getName(), 
                    String.join("\n\t", sb), content != null ? content : ""};
        }
    }
}
