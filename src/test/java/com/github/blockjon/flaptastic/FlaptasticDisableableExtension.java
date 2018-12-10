package com.github.blockjon.flaptastic;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;


import org.junit.jupiter.api.extension.ExtensionContext;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Iterator;
import java.util.ArrayList;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import com.google.gson.JsonParser;
import com.google.gson.JsonObject;
import com.google.gson.internal.LinkedTreeMap;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import java.io.File;


public class FlaptasticDisableableExtension implements ExecutionCondition {
    static HashMap disabledHashMap = null;
    static Boolean tryFlaptastic = null;
    static Boolean flaptasticActivated = null;

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

        // This shows you the path to the test class within the compiled classes area
        // Example: "/Users/jonathanblock/workspace/junit-flaptastic/target/test-classes/com/github/blockjon/flaptastic/"
        String pathToTestClass = execContext.getTestClass().get().getResource(".").getPath();

        // The likely filename of the file that the unit test was authored within.
        String unitTestClassName = execContext.getParent().get().getDisplayName();

        // This represents the path to the tests directory in the source code.
        // Example "/Users/jonathanblock/workspace/junit-flaptastic/src/test/java"
        String pathToTests = new File("").getAbsolutePath();


        String relativePathToTestClass = pathToTestClass.replaceAll("^" + pathToTests + "/", "");
        String relativePathToTestFile = relativePathToTestClass + unitTestClassName + ".class";

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
}
