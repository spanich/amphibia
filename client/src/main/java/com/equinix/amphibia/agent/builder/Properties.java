package com.equinix.amphibia.agent.builder;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

/**
 *
 * @author dgofman
 */
public class Properties {

    private JSONObject globals;
    private JSONObject project;
    private JSONObject testsuite;
    private JSONObject testcase;
    private JSONObject teststep;

    private static final Logger LOGGER = Logger.getLogger(Properties.class.getName());

    public Properties(JSONArray globals, JSONObject project) {
        this.globals = new JSONObject();
        this.project = project;
        globals.forEach((item) -> {
            JSONObject props = (JSONObject) item;
            this.globals.put(props.getString("name"), props.get("value"));
        });
    }

    public Properties(JSONObject globals, JSONObject project) {
        this.globals = globals;
        this.project = project;
    }

    public Properties setTestSuite(JSONObject testsuite) {
        this.testsuite = testsuite;
        testsuite.keySet().forEach((key) -> {
            replace(testsuite.get(key), testsuite);
        });
        return this;
    }

    public Properties setTestStep(JSONObject teststep) {
        this.teststep = teststep;
        teststep.keySet().forEach((key) -> {
            replace(teststep.get(key), teststep);
        });
        return this;
    }

    public Properties setTestCase(JSONObject testcase) {
        this.testcase = testcase;
        testcase.keySet().forEach((key) -> {
            replace(testcase.get(key), testcase);
        });
        return this;
    }

    public String replace(Object replace) {
        return replace(replace, null);
    }

    protected String replace(Object replace, JSONObject target) {
        String value = String.valueOf(replace);
        if (replace instanceof String) {
            value = value.replaceAll("\"`\\$\\{#(.*?)\\}`\"", "\\${#$1}");
            StringBuilder sb = new StringBuilder(value);
            Matcher m = Pattern.compile("\\$\\{#(.*?)#(.*?)\\}", Pattern.DOTALL | Pattern.MULTILINE).matcher(value);
            while (m.find()) {
                JSONObject source = getProperty(m.group(1));
                if (source == null) {
                } else {
                    String key = m.group(2);
                    int offset = value.length() - sb.length();
                    if (!source.containsKey(key)) {
                        sb.replace(m.start(0) - offset, m.end(2) - offset + 1, "${#" + m.group(2) + "}");
                        LOGGER.log(Level.WARNING, "Value is undefined: {0}", m.group());
                    } else if (target != null) {
                        target.put(key, source.get(key));
                    } else {
                        sb.replace(m.start(0) - offset, m.end(2) - offset + 1, String.valueOf(source.get(key)));
                    }
                }
            }
            //Replace by hierarchy
            m = Pattern.compile("\\$\\{#(.*?)\\}", Pattern.DOTALL | Pattern.MULTILINE).matcher(value);
            while (m.find()) {
                String key = m.group(1);
                Object propValue;
                if (teststep != null && teststep.containsKey(key)) {
                    propValue = teststep.get(key);
                } else if (testcase != null && testcase.containsKey(key)) {
                    propValue = testcase.get(key);
                } else if (testsuite != null && testsuite.containsKey(key)) {
                    propValue = testsuite.get(key);
                } else if (project != null && project.containsKey(key)) {
                    propValue = project.get(key);
                } else if (globals != null && globals.containsKey(key)) {
                    propValue = globals.get(key);
                } else {
                    continue;
                }
                int offset = value.length() - sb.length();
                sb.replace(m.start(0) - offset, m.end(1) - offset + 1, String.valueOf(propValue));
            }
            value = sb.toString();
        }
        return value;
    }

    public JSONObject getProperty(String name) {
        if ("Global".equals(name)) {
            return globals;
        }
        if ("Project".equals(name)) {
            return project;
        }
        if ("TestSuite".equals(name)) {
            return testsuite;
        }
        if ("TestCase".equals(name)) {
            return testcase;
        }
        if ("TestStep".equals(name)) {
            return teststep;
        }
        return null;
    }

    public Properties cloneProperties() {
        Properties clone = new Properties(globals, project);
        clone.testsuite = testsuite;
        clone.testcase = testcase;
        clone.teststep = teststep;
        return clone;
    }
}
