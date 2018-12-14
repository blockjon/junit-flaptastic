package com.github.blockjon.flaptastic;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;


import org.junit.jupiter.api.extension.ExtensionContext;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Iterator;
import java.util.ArrayList;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.DataOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import com.google.gson.JsonParser;
import com.google.gson.JsonObject;
import com.google.gson.internal.LinkedTreeMap;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import java.io.File;
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;


public class FlaptasticDisableableExtension implements ExecutionCondition, AfterTestExecutionCallback {
    private static HashMap<String, List> disabledHashMap = new HashMap<String, List>();
    private static Boolean tryFlaptastic = null;
    private static Boolean flaptasticActivated = null;
    private static JSONArray testResults = new JSONArray();

    public FlaptasticDisableableExtension() {
        if (tryFlaptastic == null) {
            if (this.sufficientEnvVarsDetected()) {
                System.out.println("Flaptastic activated.\n");
                tryFlaptastic = true;
            } else {
                tryFlaptastic = false;
                return;
            }
            this.loadDisabledTests();
        }
    }

    private boolean sufficientEnvVarsDetected() {
        String envVarName;
        String envVarValue;
        String[] envVarNames = new String[]{
                "FLAPTASTIC_API_TOKEN",
                "FLAPTASTIC_ORGANIZATION_ID",
                "FLAPTASTIC_SERVICE"
        };
        for (int i = 0; i < envVarNames.length; i++) {
            envVarName = envVarNames[i];
            envVarValue = System.getenv(envVarName);
            if(envVarValue == null) {
                return false;
            }
        }
        return true;
    }

    private void loadDisabledTests() {
        String body = this.getDisabledTestsJson();
        JsonObject jsonObject = new JsonParser().parse(body).getAsJsonObject();

        // Create a container for all of the files with test exclusions.
        HashMap<String, List> disabled = new HashMap<String, List>();

        // Here, some kind of for loop would loop over each file
        // and the tests which are ignored in that file.
        // for....
        for(LinkedTreeMap.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
            String ignoredFileName = entry.getKey();
            List testsToIgnore = new ArrayList();
            JsonElement value = entry.getValue();
            JsonArray jsonArrayOfElements = ((JsonArray) value).getAsJsonArray();
            for (int j=0; j<jsonArrayOfElements.size(); j++) {
                String testNameToIgnore = jsonArrayOfElements.get(j).getAsJsonObject().get("name").getAsString();
                HashMap<String, String> ignoredTestFunction = new HashMap<String, String>();
                ignoredTestFunction.put("name", testNameToIgnore);
                testsToIgnore.add(ignoredTestFunction);
            }
            disabled.put(ignoredFileName, testsToIgnore);
        }
        disabledHashMap = disabled;
        flaptasticActivated = true;
    }

    private String getDisabledTestsJson() {
        String organization_id = System.getenv("FLAPTASTIC_ORGANIZATION_ID");
        String service = System.getenv("FLAPTASTIC_SERVICE");
        URL urlForGetRequest;
        HttpURLConnection conection;
        int responseCode;

        try {
            urlForGetRequest = new java.net.URL("https://frontend-api.flaptastic.com/api/v1/skippedtests/" + organization_id + "/" + service);
            String readLine = null;

            conection = (HttpURLConnection) urlForGetRequest.openConnection();
            conection.setRequestProperty("Content-Type", "application/json");
            conection.setRequestProperty("Bearer", System.getenv("FLAPTASTIC_API_TOKEN"));
            conection.setConnectTimeout(5000); //set timeout to 5 seconds
            conection.setRequestMethod("GET");
            responseCode = conection.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(conection.getInputStream()));
                StringBuffer response = new StringBuffer();
                while ((readLine = in .readLine()) != null) {
                    response.append(readLine);
                } in .close();
                return response.toString();
            } else {
                throw new RuntimeException("Flaptastic returned HTTP response code " + responseCode);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext execContext) {
        // A function name like "testSomething1"
        String unitTestFunctionName = execContext.getTestMethod().get().getName();
        String relativePathToTestFile = this.getRelativePathToTestFile(execContext);


        if (this.isTestDisabled(relativePathToTestFile, unitTestFunctionName)) {
            return ConditionEvaluationResult.disabled("Disabled via flaptastic.");
        } else {
            return ConditionEvaluationResult.enabled("Flaptastic is not activated.");
        }
    }

    /**
     * Given a relative path to a source code file and test function name, determine if
     * this unit test is currently disabled.
     *
     * @param relativePathToFile
     * @param functionName
     * @return
     */
    private boolean isTestDisabled(String relativePathToFile, String functionName) {
        Iterator it = disabledHashMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            // If one of the known disabled files (pair.getKey()) is the name
            // of the file that JUnit5 wants to execute....
            if (pair.getKey() == relativePathToFile) {
                // Loop over each of the test functions to see if the JUnit5 test
                // functionName matches one of the disabled functions.
                for (int j=0; j<((ArrayList) pair.getValue()).size(); j++) {
                    if (((HashMap) ((ArrayList) pair.getValue()).get(j)).get("name") == functionName) {
                        return true;
                    }
                }
            }
            it.remove(); // avoids a ConcurrentModificationException
        }
        return false;
    }

    public void afterTestExecution(ExtensionContext context) throws Exception {
        String file = this.getRelativePathToTestFile(context);
        String name = context.getTestMethod().get().getName();
        String pkg = context.getTestMethod().get().getDeclaringClass().getName();
        Integer line = this.getLine(pkg, name);
        String status;
        String ex = null;
        JSONArray file_stack = new JSONArray();
        JSONArray exception_site = new JSONArray();

        // If an exception is detected...
        if (context.getExecutionException().isPresent()) {
            status = "failed";
            ex = context.getExecutionException().toString();
            for (int i=0; i<context.getExecutionException().get().getStackTrace().length; i++) {
                file_stack.add(context.getExecutionException().get().getStackTrace()[i].getFileName() + " (" + context.getExecutionException().get().getStackTrace()[i].getClassName() + ")");
            }

        } else {
            status = "passed";
        }

        JSONObject obj = new JSONObject();
        obj.put("status", status);
        obj.put("file", file);
        obj.put("line", line);
        obj.put("name", name);
        obj.put("exception", ex);
        obj.put("file_stack", file_stack);
        obj.put("exception_site", exception_site);
        testResults.add(obj);

        this.sendQueueToIngest();
    }

    private void sendQueueToIngest() {
        String query = "https://frontend-api.flaptastic.com/api/v1/ingest";

        Long ts = System.currentTimeMillis() / 1000L;

        JSONObject obj = new JSONObject();
        obj.put("branch", System.getenv("FLAPTASTIC_BRANCH"));
        obj.put("commit_id", System.getenv("FLAPTASTIC_COMMIT_ID"));
        obj.put("link", System.getenv("FLAPTASTIC_LINK"));
        obj.put("organization_id", System.getenv("FLAPTASTIC_ORGANIZATION_ID"));
        obj.put("service", System.getenv("FLAPTASTIC_SERVICE"));
        obj.put("timestamp", ts);
        obj.put("test_results", testResults);

        String jsonText = obj.toString();

        try {
            URL url = new java.net.URL(query);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(5000);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setRequestProperty("Bearer", System.getenv("FLAPTASTIC_API_TOKEN"));
            conn.setDoOutput(true);
            conn.setDoInput(true);

            DataOutputStream dOs = new DataOutputStream(conn.getOutputStream());
            dOs.write(jsonText.getBytes("UTF-8"));
            dOs.close();

            Integer responseCode = conn.getResponseCode();

            conn.disconnect();

            if (responseCode != 201) {
                throw new Exception("Failure to deliver flaps. Response code " + responseCode.toString() + " returned.");
            }
        } catch (Exception e) {
            System.out.println("Problem detected when delivering flaps: " + e.toString());
        }
        // Clear the buffer.
        testResults = new JSONArray();
    }

    private String getRelativePathToTestFile(ExtensionContext context) {
        // This shows you the path to the test class within the compiled classes area
        // Example: "/Users/jonathanblock/workspace/junit-flaptastic/target/test-classes/com/github/blockjon/flaptastic/"
        String pathToTestClass = context.getTestClass().get().getResource(".").getPath();

        // The likely filename of the file that the unit test was authored within.
        String unitTestClassName = context.getParent().get().getDisplayName();

        // This represents the path to the tests directory in the source code.
        // Example "/Users/jonathanblock/workspace/junit-flaptastic/src/test/java"
        String pathToTests = new File("").getAbsolutePath();


        String relativePathToTestClass = pathToTestClass.replaceAll("^" + pathToTests + "/", "");
        String relativePathToTestFile = relativePathToTestClass + unitTestClassName + ".class";
        return relativePathToTestFile;
    }

    private Integer getLine(String pkg, String name) {
        ClassPool pool = ClassPool.getDefault();
        try {
            CtClass cc = pool.get(pkg);
            CtMethod methodX = cc.getDeclaredMethod(name);
            int lineNumber = methodX.getMethodInfo().getLineNumber(0);
            return lineNumber;
        } catch (Exception e) {
            return 0;
        }
    }
}
