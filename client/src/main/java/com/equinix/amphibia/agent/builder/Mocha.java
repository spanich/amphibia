package com.equinix.amphibia.agent.builder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipOutputStream;

import org.apache.commons.cli.CommandLine;

import com.equinix.amphibia.agent.converter.Profile;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

public class Mocha extends ProjectAbstract {

    public static final String DEFAULT_ENDPOINT = "${globals.RestEndPoint}";

    protected File outputFile;
    protected File zipFile;
    protected File packageFile;
    protected String packageJSON;
    protected String testsJS;

    private static final Logger LOGGER = Logger.getLogger(Mocha.class.getName());

    public Mocha(CommandLine cmd) throws Exception {
        super(cmd);
    }

    @Override
    protected void readInputData() throws Exception {
        super.readInputData();
        packageJSON = this.getFileContent(getTemplateFile("mocha/package.json"));
        testsJS = this.getFileContent(getTemplateFile("mocha/tests.js"));
    }

    @Override
    protected void buildProject(String name) throws Exception {
        super.buildProject(name);
        outputFile = new File(outputDirPath, name + ".js");
        zipFile = new File(outputDirPath, name + ".zip");
        packageJSON = replace(packageJSON, "<% PROJECT_NAME %>", name);
    }

    @Override
    protected void saveFile() throws Exception {
        super.saveFile();
        PrintWriter writer;

        packageFile = new File(outputDirPath, "package.json");
        writer = new PrintWriter(new FileOutputStream(packageFile, false));
        writer.println(packageJSON);
        writer.close();
        LOGGER.log(Level.INFO, "The package.json file saved successfully.\n{0}", packageFile);

        writer = new PrintWriter(new FileOutputStream(outputFile, false));
        writer.println(testsJS);
        writer.close();
        Builder.addResult(Builder.ADD_PROJECT, outputFile);
    }

    @Override
    protected void saveResources() throws Exception {
        ZipOutputStream zout = new ZipOutputStream(new FileOutputStream(zipFile));
        addToZip(outputFile, zout, outputDirPath);
        addToZip(packageFile, zout, outputDirPath);
        if (!"false".equals(cmd.getOptionValue(Builder.RESOURCE))) {
            addToZip(new File(projectDirPath, Profile.DATA_DIR), zout, outputDirPath);
        }
        zout.close();
        Builder.addResult(Builder.ADD_RESOURCE, zipFile);
    }

    @Override
    protected void printEnd() throws Exception {
        String note = "Open a command prompt and navigate to the folder containing the file \"{0}\""
                + "\nRun following commands: \nnpm install\nnpm start";
        Object[] args = new String[]{outputFile.getName()};
        Builder.addResult(Builder.ADD_NOTE, note);
        Builder.addResult(Builder.ADD_ARGS, args);
        LOGGER.log(Level.INFO, "Saved successfully.\n\nNOTE:\n\n");
        LOGGER.log(Level.INFO, MessageFormat.format(note, args));
    }

    @Override
    protected void buildGlobalParameters(JSONArray globals) throws Exception {
        super.buildGlobalParameters(globals);
        List<String> globalConfig = new ArrayList<>();
        List<String> globalConst = new ArrayList<>();
        globals.forEach((item) -> {
            JSONObject globalItem = (JSONObject) item;
            String parameterJSON = "\t\t\"<% NAME %>\": <% VALUE %>";
            parameterJSON = replace(parameterJSON, "<% NAME %>", globalItem.get("name"));
            parameterJSON = replace(parameterJSON, "<% VALUE %>", getValue(globalItem.get("value"), "\""));
            globalConfig.add(parameterJSON);

            parameterJSON = "\t'<% NAME %>': process.env['npm_package_config_<% NAME %>']";
            parameterJSON = replace(parameterJSON, "<% NAME %>", globalItem.get("name"));
            globalConst.add(parameterJSON);
        });
        packageJSON = replace(packageJSON, "<% GLOBALS %>", String.join(",\n", globalConfig));
        testsJS = replace(testsJS, "<% GLOBALS %>", String.join(",\n", globalConst));
    }

    @Override
    protected void buildProperties(JSONObject properties) throws Exception {
        super.buildProperties(properties);
    }

    @Override
    protected void buildInterfaces(JSONArray interfaces) throws Exception {
        super.buildInterfaces(interfaces);
        List<String> resourceList = new ArrayList<>();
        interfaces.forEach((item) -> {
            JSONObject interfaceItem = (JSONObject) item;
            interfacesJson.element(interfaceItem.getString("id"), interfaceItem);
            String headerJSON = "\t'" + interfaceItem.getString("name") + "': {\n<% PARAMETERS %>\n\t}";

            if (interfaceItem.containsKey("headers")) {
                JSONObject headers = interfaceItem.getJSONObject("headers");
                List<String> parameterList = new ArrayList<>();
                headers.keySet().forEach((key) -> {
                    String parameterJSON = "\t\t'<% NAME %>': <% VALUE %>";
                    parameterJSON = replace(parameterJSON, "<% NAME %>", key);
                    parameterJSON = replace(parameterJSON, "<% VALUE %>", getValue(headers.get(key)));
                    parameterList.add(parameterJSON);
                });
                headerJSON = replace(headerJSON, "<% PARAMETERS %>", String.join(",\n", parameterList));
            }
            resourceList.add(headerJSON);
        });
        testsJS = replace(testsJS, "<% HEADERS %>", String.join(",\n", resourceList));
    }

    @Override
    protected void buildResources(JSONArray resources) throws Exception {
        super.buildResources(resources);

        Properties properties = new Properties(globalsJson, projectPropertiesJSON);

        List<String> testLists = new ArrayList<>();
        for (Object item : resources) {
            JSONObject resource = (JSONObject) item;
            JSONObject testsuites = resource.getJSONObject("testsuites");
            JSONObject interfaceItem = (JSONObject) interfacesJson.getOrDefault(resource.getString("interfaceId"), null);
            for (Object name : testsuites.keySet()) {
                JSONObject testSuiteItem = testsuites.getJSONObject(name.toString());
                properties.setTestSuite(testSuiteItem.getJSONObject("properties"));

                String test = this.getFileContent(getTemplateFile("mocha/test.js"));

                test = replace(test, "<% TESTSUITE_NAME %>", name);
                test = replace(test, "<% ENDPOINT %>", resource.getString("endpoint"));
                test = replace(test, "<% INTERFACE %>", interfaceItem != null ? interfaceItem.getString("name") : "");

                List<String> testCaseList = new ArrayList<>();
                buildTestCases(testCaseList, testSuiteItem.getJSONArray("testcases"), properties);
                test = replace(test, "<% TESTCASES %>", String.join("\n\n", testCaseList));
                testLists.add(test);
            }
        }
        testsJS = replace(testsJS, "<% TESTS %>", String.join("\n\n", testLists));
    }

    protected void buildTestCases(List<String> testCaseList, JSONArray testcases, Properties properties) throws Exception {
        for (Object item : testcases) {
            JSONObject testCaseItem = (JSONObject) item;
            if ("restrequest".equals(testCaseItem.get("type"))) {
                properties.setTestCase(testCaseItem.getJSONObject("properties"));

                String testcase = this.getFileContent(getTemplateFile("mocha/testcase.js"));
                testcase = replace(testcase, "<% SUMMARY %>", testCaseItem.getString("summary"));
                testcase = replace(testcase, "<% TESTCASE_NAME %>", testCaseItem.getString("name"));

                if (testCaseItem.containsKey("config")) {
                    JSONObject config = testCaseItem.getJSONObject("config");
                    JSONObject replace = config.getJSONObject("replace");
                    if (replace != null) {
                        testcase = replace(testcase, "<% ENDPOINT %>", replace.containsKey("endpoint") ? "headers['" + replace.get("endpoint") + "']" : "${endpoint}");
                        testcase = replace(testcase, "<% METHOD %>", replace.getString("method"));
                        testcase = replace(testcase, "<% METHOD_NAME %>", replace.getString("method").toLowerCase());

                        Object path = replace.get("path");
                        if (path != null) {
                            path = properties.replace(path.toString()).replaceAll("&amp;", "&");
                            testcase = replace(testcase, "<% PATH %>", path);
                        }

                        Object body = replace.get("body");
                        if (!isNULL(body)) {
                            body = "JSON.parse(`" + properties.replace(prettyJson(body)) + "`)";
                        }

                        testcase = replace(testcase, "<% BODY %>", body);

                        for (Object key : replace.keySet()) {
                            Object value = replace.get(key);
                            testcase = replace(testcase, "<% " + key.toString().toUpperCase() + " %>", value instanceof String ? value : toJson(value));
                        }

                        testcase = replace(testcase, "<% MEDIATYPE %>", replace.containsKey("mediatype") ? replace.getString("mediatype") : "application/json");

                        if (config.containsKey("assertions")) {
                            List<String> assertionList = new ArrayList<>();
                            for (Object assertion : config.getJSONArray("assertions")) {
                                JSONObject assertionItem = (JSONObject) assertion;
                                String type = assertionItem.getString("type");
                                String line = "";
                                replace = assertionItem.getJSONObject("replace");

                                switch (type) {
                                    case "HTTPHeaderEquals":
                                        line = "assert.equal(res.header['<% NAME %>'], '<% VALUE %>');";
                                        break;
                                    case "InvalidHTTPStatusCodes":
                                        line = "assert.notEqual(res.statusCode, <% VALUE %>);";
                                        break;
                                    case "ValidHTTPStatusCodes":
                                        line = "assert.equal(res.statusCode, <% VALUE %>);";
                                        break;
                                }
                                for (Object key : replace.keySet()) {
                                    line = replace(line, "<% " + key.toString().toUpperCase() + " %>", replace.get(key).toString());
                                }
                                assertionList.add(line);
                            }
                            testcase = replace(testcase, "<% ASSERTIONS %>", String.join(",\n", assertionList));
                        }
                    }
                }
                testCaseList.add(testcase);
            }
        }
    }

    @Override
    protected Object getValue(Object value) {
        return getValue(value, "'");
    }
}
