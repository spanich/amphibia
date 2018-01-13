package com.equinix.amphibia.agent.converter;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.io.IOUtils;

import com.equinix.amphibia.agent.converter.Converter.RESOURCE_TYPE;
import java.util.HashMap;

import net.sf.json.JSONArray;
import net.sf.json.JSONNull;
import net.sf.json.JSONObject;

public final class Swagger {

    protected CommandLine cmd;
    protected JSONObject doc;
    protected JSONObject output;
    protected JSONObject swaggerProperties;
    protected Runner runner;

    public static final JSONObject asserts = new JSONObject();
    public static final JSONNull NULL = JSONNull.getInstance();

    public Swagger(CommandLine cmd, InputStream input, InputStream properties, JSONObject output, Runner runner)
            throws Exception {
        this.cmd = cmd;
        this.doc = getContent(input);
        this.swaggerProperties = getContent(properties);
        this.output = output;
        this.runner = runner;
    }

    public String init(String name, int index) throws Exception {
        if (name == null) {
            JSONObject info = doc.getJSONObject("info");
            if (!info.isNullObject()) {
                name = (String) info.getString("title").replaceAll(" ", "");
            }
        }
        if (name == null || name.trim().length() == 0) {
            name = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        }
        if (!output.containsKey("name")) {
            output.accumulate("name", name);
        }
        runner.setDefinition(doc);
        parse(index);
        return name;
    }

    public static String getJson(List<?> value) throws Exception {
        return getJson(JSONArray.fromObject(value).toString());
    }

    public static String getJson(Map<?, ?> value) throws Exception {
        return getJson(JSONObject.fromObject(value).toString());
    }

    public static String getJson(String strJson) throws Exception {
        ScriptEngine scriptEngine = new ScriptEngineManager().getEngineByName("JavaScript");
        scriptEngine.put("jsonString", strJson);
        scriptEngine.eval("result = JSON.stringify(JSON.parse(jsonString), null, 4)");
        return ((String) scriptEngine.get("result")).replaceAll(" {4}", "\t");
    }

    public static String escapeJson(Map<?, ?> value) throws Exception {
        String output = getJson(value);
        Pattern p = Pattern.compile("(\\\"'\\$\\{#)(.*)(}'\")");
        Matcher m = p.matcher(output);
        if (m.find()) {
            output = m.replaceAll("\\$\\{#$2}");
        }
        return output;
    }

    protected void parse(int index) throws Exception {
        JSONArray schemes = new JSONArray();
        if (doc.containsKey("schemes")) {
            schemes = doc.getJSONArray("schemes");
        } else {
            schemes.add("http");
        }
        String[] pair = doc.getString("host").split(":");
        String host = schemes.get(0) + "://" + pair[0] + ':'
                + ((pair.length == 2 ? pair[1] : "http".equals(schemes.get(0)) ? 80 : 443));

        JSONArray hosts = output.containsKey("hosts") ? output.getJSONArray("hosts") : new JSONArray();
        JSONArray globals = output.containsKey("globals") ? output.getJSONArray("globals") : new JSONArray();
        if (swaggerProperties != null) {
            JSONObject propertyGlobals = swaggerProperties.getJSONObject("globalProperties");
            for (Object key : propertyGlobals.keySet()) {
                if (!"RestEndPoint".equals(key)) {
                    boolean newProp = true;
                    for (int i = 0; i < globals.size(); i++) {
                        if (key.equals(globals.getJSONObject(i).getString("name"))) {
                            globals.getJSONObject(i).accumulate("value", propertyGlobals.get(key));
                            newProp = false;
                            break;
                        }
                    }
                    if (newProp) {
                        globals.add(new HashMap<String, Object>() {
                            {
                                put("name", key);
                                put("value", propertyGlobals.get(key));
                            }
                        });
                    }
                } else {
                    host = propertyGlobals.getString(key.toString());
                    globals.add(new HashMap<String, Object>() {
                        {
                            put("name", "RestEndPoint" + index);
                            put("value", propertyGlobals.get(key));
                            put("type", "endpoint");
                        }
                    });
                }
            }
        } else {
            final String hostVal = host;
            globals.add(new HashMap<String, Object>() {
                {
                    put("name", "RestEndPoint" + index);
                    put("value", hostVal);
                    put("type", "endpoint");
                }
            });
        }

        if (!hosts.contains(host)) {
            hosts.add(host);
        }
        output.element("hosts", hosts);
        output.element("globals", globals);

        JSONArray interfaces = output.containsKey("interfaces") ? output.getJSONArray("interfaces") : new JSONArray();
        String interfaceBasePath = doc.getString("basePath");
        String interfaceName = interfaceBasePath;
        String param = cmd.getOptionValue(Converter.INTERFACES);
        if (param != null) {
            String[] params = param.split(",");
            if (params.length > index && !params[index].isEmpty()) {
                interfaceName = params[index];
            }
        }

        JSONObject headers = new JSONObject();
        if (swaggerProperties != null) {
            headers = swaggerProperties.getJSONObject("headers");
        } else {
            headers.element("CONTENT-TYPE", "application/json");
        }

        final String name = interfaceName;
        final JSONObject hs = headers;
        interfaces.add(new HashMap<String, Object>() {
            {
                put("name", name);
                put("basePath", interfaceBasePath);
                put("type", "rest");
                put("headers", hs);
            }
        });
        output.element("interfaces", interfaces);

        JSONArray projectResources = output.containsKey("projectResources") ? output.getJSONArray("projectResources") : new JSONArray();
        JSONObject testsuites = output.containsKey("testsuites") ? output.getJSONObject("testsuites") : new JSONObject();

        addTestSuite(index, interfaceName, interfaceBasePath, testsuites);

        JSONObject properties = output.containsKey("properties") ? output.getJSONObject("properties") : new JSONObject();
        if (swaggerProperties != null) {
            JSONObject projectProperties = swaggerProperties.getJSONObject("projectProperties");
            projectProperties.keySet().forEach((key) -> {
                Object value = projectProperties.get(key);
                if (properties.containsKey(key)) {
                    boolean isEquials;
                    if (value instanceof String) {
                        isEquials = value.toString().equals(properties.get(key));
                    } else {
                        isEquials = value == properties.get(key);
                    }
                    if (!isEquials) {
                        Converter.addResult(RESOURCE_TYPE.warnings, "conflicting/ambiguous property name - " + key);
                    }
                }
                properties.element(key.toString(), value);
            });
        }
        output.put("properties", properties);

        final String iName = interfaceName;
        projectResources.add(new HashMap<String, Object>() {
            {
                put("endpoint", "RestEndPoint" + index);
                put("interface", iName);
                put("testsuites", testsuites);
            }
        });
        output.element("projectResources", projectResources);
    }

    protected void addTestSuite(int index, String interfaceName, String interfaceBasePath, JSONObject testsuites) throws Exception {
        JSONObject paths = doc.getJSONObject("paths");
        Map<String, List<ApiInfo>> testSuiteMap = new TreeMap<>();
        paths.keySet().forEach((path) -> {
            String originalURL = path.toString();
            JSONObject apis = paths.getJSONObject(originalURL);
            apis.keySet().forEach((methodName) -> {
                if (!"parameters".equals(methodName)) {
                    JSONObject api = apis.getJSONObject(methodName.toString());
                    String apiName = stripName(path.toString());
                    String testSuiteName = "TestSuite-" + testSuiteMap.size();
                    if (api.containsKey("tags")) {
                        JSONArray tags = api.getJSONArray("tags");
                        if (tags.size() > 0) {
                            testSuiteName = (String) tags.get(0);
                        }
                    }
                    testSuiteName = Swagger.stripName(testSuiteName);
                    List<ApiInfo> apiList = testSuiteMap.get(testSuiteName);
                    if (apiList == null) {
                        apiList = new ArrayList<>();
                        testSuiteMap.put(testSuiteName, apiList);
                    }
                    apiList.add(new ApiInfo(interfaceName, interfaceBasePath, testSuiteName, methodName.toString(), apiName, originalURL, api, apis));
                }
            });
        });

        for (String testSuiteName : testSuiteMap.keySet()) {
            JSONArray testcases = new JSONArray();
            JSONObject properties = new JSONObject();
            if (swaggerProperties != null) {
                JSONObject testSuiteProperties = swaggerProperties.getJSONObject("testSuiteProperties");
                if (testSuiteProperties.containsKey(testSuiteName)) {
                    properties = testSuiteProperties.getJSONObject(testSuiteName);
                }
            }
            addTestCases(index, testcases, testSuiteMap.get(testSuiteName));
            final JSONObject props = properties;
            testsuites.put(testSuiteName, new HashMap<String, Object>() {
                {
                    put("properties", props);
                    put("testcases", testcases);
                }
            });
        }

        this.runner.addTestCases(index, interfaceName, testSuiteMap);
    }

    protected void addTestCases(int index, JSONArray testcases, List<ApiInfo> apiList) throws Exception {
        for (ApiInfo info : apiList) {
            String summaryInfo = info.apiName;
            if (info.api.containsKey("summary")) {
                summaryInfo = info.api.getString("summary");
            } else if (info.api.containsKey("summary")) {
                summaryInfo = info.api.getString("summary");
            }
            info.testCaseName = info.methodName + "_" + info.apiName;
            Map<String, Object> properties = new LinkedHashMap<>();
            String summary = summaryInfo;
            testcases
                    .add(new HashMap<String, Object>() {
                        {
                            put("type", "restrequest");
                            put("name", info.testCaseName);
                            put("summary", summary);
                            put("properties", properties);
                            put("config", getConfig(index, properties, info));
                        }
                    });
        }
    }

    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    protected JSONObject getConfig(int index, Map<String, Object> properties, ApiInfo info) throws Exception {
        JSONObject api = info.api;
        JSONObject config = new JSONObject();

        JSONArray assertions = new JSONArray();
        JSONObject responses = api.getJSONObject("responses");
        for (Object httpCode : responses.keySet()) {
            properties.put("HTTPStatusCode", Integer.parseInt(httpCode.toString()));
            assertions.add(new HashMap<String, Object>() {
                {
                    put("type", "ValidHTTPStatusCodes");
                    put("replace", new HashMap<String, String>() {
                        {
                            put("value", "${#HTTPStatusCode}");
                        }
                    });
                }
            });
            break;
        }
        config.accumulate("assertions", assertions);

        JSONArray statuses = null;
        if (swaggerProperties != null && swaggerProperties.containsKey("asserts")) {
            statuses = swaggerProperties.getJSONArray("asserts");
        }
        for (Object httpCode : responses.keySet()) {
            int code;
            try {
                code = Integer.parseInt(httpCode.toString());
            } catch (NumberFormatException e) {
                Converter.addResult(RESOURCE_TYPE.errors, "Invalid HTTP code [" + httpCode + "]. " + info.methodName + " :: " + info.apiName);
                continue;
            }
            JSONObject response = responses.getJSONObject(httpCode.toString());
            if (response != null && statuses != null) {
                JSONObject item = new JSONObject();
                for (Object obj : statuses) {
                    JSONObject jsonObj = (JSONObject) obj;
                    JSONArray range = jsonObj.getJSONArray("range");
                    if (code >= (int) range.get(0) && code <= (int) range.get(1)) {
                        item.put("status", jsonObj.getString("status"));
                        item.put("statusCode", "${#HTTPStatusCode}");

                        if (!"false".equals(Converter.cmd.getOptionValue(Converter.SCHEMA))) {
                            asserts.put(httpCode.toString(), item);
                        }

                        if (code >= 200 && code < 300) {
                            if (response.containsKey("schema") && response.getJSONObject("schema").containsKey("$ref")) {
                                new Schema(this, response.getJSONObject("schema").getString("$ref"), "responses");
                            }
                        }
                        break;
                    }
                }
            }
        }

        Definition definition = new Definition(doc, this);
        parseDefinition(info, definition, info.apis, properties);
        parseDefinition(info, definition, api, properties);
        JSONObject body = api.getJSONObject("example");
        if (body.isNullObject()) {
            body = definition.getExample();
        }

        String path = info.path;
        for (String name : definition.getParameters().keySet()) {
            String paramValue = definition.getParameters().get(name);
            if (paramValue != null) {
                path = path.replaceAll("\\{" + name + "\\}", paramValue);
            }
        }

        config.accumulate("operationId", api.getString("operationId"));
        if (definition.ref != null) {
            config.accumulate("definition", definition.ref.split("#/definitions/")[1]);
        }

        final String replacePath = info.interfaceBasePath.substring(1) + path + definition.getQueries();
        final Object replaceBody = body == null ? NULL : body;
        config.accumulate("replace",
                new HashMap<String, Object>() {
            {
                put("method", info.methodName);
                put("path", replacePath);
                put("body", replaceBody);
            }
        });
        return config;
    }

    @SuppressWarnings("unchecked")
    protected void parseDefinition(ApiInfo info, Definition definition, JSONObject api, Map<String, Object> properties) throws Exception {
        if (api.containsKey("parameters")) {
            String methodName = info.methodName;
            for (Object item : api.getJSONArray("parameters")) {
                JSONObject param = (JSONObject) item;
                String in = null;
                if (param.containsKey("in")) {
                    in = param.getString("in");
                }
                String type = null;
                if (param.containsKey("type")) {
                    type = param.getString("type");
                }
                if (null != in) {
                    switch (in) {
                        case "body":
                            JSONObject schema = param.getJSONObject("schema");
                            Schema newSchema = null;
                            if (schema.containsKey("$ref")) {
                                definition.ref = schema.getString("$ref");
                                definition.getRef(definition.ref);
                                newSchema = new Schema(this, definition.ref, "requests");
                            }
                            JSONObject example = definition.getExample();
                            if (newSchema != null && example != null) {
                                Validator.validateExample(schema.getString("$ref"), (Map<Object, Object>) example, newSchema);
                            }
                            break;
                        case "query":
                            if (param.containsKey("default") || param.containsKey("enum")) {
                                definition.addQueryParam(info, param.getString("name"), Definition.getEnumOrDefault(param), properties, type);
                            } else {
                                definition.addQueryParam(info, param.getString("name"), properties, type);
                            }
                            break;
                        case "path":
                            definition.addParameter(info, param.getString("name"), Definition.getEnumOrDefault(param), properties, param.getString("type"));
                            break;
                        default:
                            break;
                    }
                }

                Validator.validatePathsParam(methodName + " - " + info.path, doc, param);
            }
        }
    }

    protected JSONObject getContent(InputStream is) throws IOException {
        if (is == null) {
            return null;
        }
        return JSONObject.fromObject(IOUtils.toString(is));
    }

    public static String getPath(File path) {
        return path.getPath().replaceAll("\\\\", "/");
    }

    public static String getDefinitionName(String ref) {
        return ref.split("/")[2];
    }

    public static String stripName(String name) {
        if (name.charAt(0) == '/') {
            name = name.substring(1);
        }
        return name.replaceAll("[ -\\/\\.]", "_").replaceAll("\\{", "").replaceAll("\\}", "").replaceAll("_+", "_");
    }

    public JSONObject getDoc() {
        return doc;
    }

    public JSONObject getSwaggerProperties() {
        return swaggerProperties;
    }

    @SuppressWarnings("NonPublicExported")
    public Object getTestRuleProperty(ApiInfo info, String id) {
        if (swaggerProperties != null) {
            JSONObject testSuiteProperties;
            if (swaggerProperties.getJSONObject("testSuiteProperties").containsKey(info.testSuiteName)) {
                testSuiteProperties = swaggerProperties.getJSONObject("testSuiteProperties").getJSONObject(info.testSuiteName);
                if (testSuiteProperties.containsKey(id)) {
                    return "${#TestSuite#" + id + "}";
                }
            }
            if (swaggerProperties.getJSONObject("properties").containsKey(id)) {
                return "${#Project#" + id + "}";
            }
        }
        return null;
    }

    final class ApiInfo {

        String interfaceName;
        String interfaceBasePath;
        String testSuiteName;
        String methodName;
        String apiName;
        String path;
        JSONObject api;
        JSONObject apis;

        String testCaseName;

        public ApiInfo(String interfaceName, String interfaceBasePath, String testSuiteName, String methodName, String apiName, String path, JSONObject api, JSONObject apis) {
            this.interfaceName = interfaceName;
            this.interfaceBasePath = interfaceBasePath;
            this.testSuiteName = testSuiteName;
            this.methodName = methodName.toUpperCase();
            this.apiName = apiName;
            this.path = path;
            this.api = api;
            this.apis = apis;
        }

        @Override
        public String toString() {
            return "ApiInfo [interfaceName=" + interfaceName + ", interfaceBasePath=" + interfaceBasePath
                    + ", testSuiteName=" + testSuiteName + ", methodName=" + methodName + ", apiName=" + apiName
                    + ", path=" + path + ", api=" + api + ", apis=" + apis + ", testCaseName=" + testCaseName + "]";
        }

    }
}
