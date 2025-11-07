package org.apache.ofbiz.multitenant.service;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.ServiceUtil;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.File;
import java.util.Map;
import java.util.HashMap;

public class DatabaseService {
    private static final String MODULE = DatabaseService.class.getName();

    public static Map<String, Object> executeDbCreationScript(DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> result = ServiceUtil.returnSuccess();
        
        // Get and validate dbName
        String dbName = (String) context.get("dbName");
        if (dbName == null || dbName.trim().isEmpty()) {
            Debug.logError("Database name is required", MODULE);
            return ServiceUtil.returnError("Database name is required");
        }
        dbName = dbName.trim().toLowerCase(); // Normalize database name

        // Get parameters with defaults from service definition
        String dbUser = (String) context.get("dbUser");
        String dbHost = (String) context.get("dbHost");
        String dbPort = (String) context.get("dbPort");

        try {
            // Set environment variables for the script
            String scriptPath = System.getProperty("ofbiz.home") + "/runsql.sh";
            Debug.logInfo("Script path: " + scriptPath, MODULE);

            // Ensure script exists and is executable
            File scriptFile = new File(scriptPath);
            if (!scriptFile.exists()) {
                Debug.logError("Script file not found: " + scriptPath, MODULE);
                return ServiceUtil.returnError("Database creation script not found");
            }
            if (!scriptFile.canExecute()) {
                Debug.logError("Script file not executable: " + scriptPath, MODULE);
                scriptFile.setExecutable(true);
            }

            ProcessBuilder processBuilder = new ProcessBuilder("/bin/bash", scriptPath);

            // Set environment variables
            Map<String, String> env = processBuilder.environment();
            env.put("DB_NAME", dbName);
            if (dbUser != null) env.put("DB_USER", dbUser);
            if (dbHost != null) env.put("DB_HOST", dbHost);
            if (dbPort != null) env.put("DB_PORT", dbPort);

            Debug.logInfo("Executing script with DB_NAME=" + dbName +
                         ", DB_USER=" + dbUser +
                         ", DB_HOST=" + dbHost +
                         ", DB_PORT=" + dbPort, MODULE);

            // Redirect error stream to output stream
            processBuilder.redirectErrorStream(true);

            // Start the process
            Process process = processBuilder.start();

            // Read the output
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                    Debug.logInfo("Script output: " + line, MODULE);
                }
            }

            // Wait for the process to complete
            int exitCode = process.waitFor();

            if (exitCode == 0) {
                result.put("result", "SUCCESS");
                result.put("output", output.toString());
            } else {
                Debug.logError("Script failed with exit code: " + exitCode + "\nOutput: " + output.toString(), MODULE);
                result = ServiceUtil.returnError("Script execution failed with exit code: " + exitCode
                    + "\nOutput: " + output.toString());
            }

        } catch (Exception e) {
            Debug.logError(e, "Error executing database creation script", MODULE);
            result = ServiceUtil.returnError("Error executing database creation script: " + e.getMessage());
        }

        return result;
    }
}
