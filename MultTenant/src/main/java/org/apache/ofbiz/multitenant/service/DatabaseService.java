package org.apache.ofbiz.multitenant.service;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.ServiceUtil;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.sql.ResultSet;
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

    /**
     * Check if a database exists on the PostgreSQL server
     * @param dctx DispatchContext
     * @param context Map containing dbName, dbHost, dbPort, dbUser, dbPassword
     * @return Map with status and exists flag
     */
    public static Map<String, Object> checkDatabaseExists(DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> result = ServiceUtil.returnSuccess();

        String dbName = (String) context.get("dbName");
        String dbHost = (String) context.get("dbHost");
        String dbPort = (String) context.get("dbPort");
        String dbUser = (String) context.get("dbUser");
        String dbPassword = (String) context.get("dbPassword");

        // Set defaults if not provided
        if (dbHost == null || dbHost.trim().isEmpty()) dbHost = "localhost";
        if (dbPort == null || dbPort.trim().isEmpty()) dbPort = "5432";
        if (dbName == null || dbName.trim().isEmpty()) {
            Debug.logError("Database name is required", MODULE);
            return ServiceUtil.returnError("Database name is required");
        }

        Connection connection = null;
        try {
            // Load PostgreSQL driver
            Class.forName("org.postgresql.Driver");

            // Create connection URL to PostgreSQL server (connect to default 'postgres' database)
            String url = "jdbc:postgresql://" + dbHost + ":" + dbPort + "/postgres";

            Debug.logInfo("Attempting to connect to PostgreSQL server at " + dbHost + ":" + dbPort, MODULE);

            // Connect to PostgreSQL server
            connection = DriverManager.getConnection(url, dbUser, dbPassword);

            // Query to check if database exists
            String checkQuery = "SELECT datname FROM pg_database WHERE datname = '" + dbName + "'";

            try (Statement statement = connection.createStatement();
                 ResultSet resultSet = statement.executeQuery(checkQuery)) {

                boolean exists = resultSet.next();

                result.put("exists", exists);
                result.put("dbName", dbName);
                result.put("dbHost", dbHost);
                result.put("dbPort", dbPort);

                if (exists) {
                    Debug.logInfo("Database '" + dbName + "' exists on " + dbHost + ":" + dbPort, MODULE);
                    result.put("status", "CONNECTED");
                    result.put("message", "Database exists and is accessible");
                } else {
                    Debug.logInfo("Database '" + dbName + "' does not exist on " + dbHost + ":" + dbPort, MODULE);
                    result.put("status", "NOT_FOUND");
                    result.put("message", "Database does not exist");
                }
            }

        } catch (ClassNotFoundException e) {
            Debug.logError("PostgreSQL JDBC driver not found", MODULE);
            result = ServiceUtil.returnError("PostgreSQL JDBC driver not found: " + e.getMessage());
            result.put("status", "ERROR");
            result.put("exists", false);
        } catch (Exception e) {
            Debug.logError(e, "Error checking database existence", MODULE);
            result = ServiceUtil.returnError("Error checking database: " + e.getMessage());
            result.put("status", "ERROR");
            result.put("exists", false);
            result.put("message", e.getMessage());
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (Exception e) {
                    Debug.logError(e, "Error closing database connection", MODULE);
                }
            }
        }

        return result;
    }

    /**
     * Get database status for display on UI
     * Returns readable status and connection info
     */
    public static Map<String, Object> getDatabaseStatus(DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> result = ServiceUtil.returnSuccess();

        String tenantId = (String) context.get("tenantId");

        if (tenantId == null || tenantId.trim().isEmpty()) {
            result.put("status", "UNKNOWN");
            result.put("statusDisplay", "No Tenant Selected");
            return result;
        }

        // Get database info from tenant's data source config
        String dbName = (String) context.get("dbName");
        String dbHost = (String) context.get("dbHost");
        String dbPort = (String) context.get("dbPort");
        String dbUser = (String) context.get("dbUser");
        String dbPassword = (String) context.get("dbPassword");

        // Call the check method
        Map<String, Object> checkResult = checkDatabaseExists(dctx, context);

        String status = (String) checkResult.get("status");
        boolean exists = (boolean) checkResult.get("exists");

        result.put("status", status);
        result.put("exists", exists);
        result.put("dbName", dbName);
        result.put("dbHost", dbHost);
        result.put("dbPort", dbPort);

        if ("CONNECTED".equals(status)) {
            result.put("statusDisplay", "✓ Connected");
            result.put("statusClass", "status-connected");
        } else if ("NOT_FOUND".equals(status)) {
            result.put("statusDisplay", "✗ Database Not Found");
            result.put("statusClass", "status-not-found");
        } else {
            result.put("statusDisplay", "⚠ Connection Error");
            result.put("statusClass", "status-error");
        }

        return result;
    }

    /**
     * Test PostgreSQL connection
     * Returns true if connection successful, false otherwise
     */
    public static boolean testPostgresConnection(String dbHost, String dbPort, String dbUser, String dbPassword) {
        if (dbHost == null || dbHost.trim().isEmpty()) dbHost = "localhost";
        if (dbPort == null || dbPort.trim().isEmpty()) dbPort = "5432";

        Connection connection = null;
        try {
            Class.forName("org.postgresql.Driver");
            String url = "jdbc:postgresql://" + dbHost + ":" + dbPort + "/postgres";
            connection = DriverManager.getConnection(url, dbUser, dbPassword);
            Debug.logInfo("PostgreSQL connection test successful", MODULE);
            return true;
        } catch (Exception e) {
            Debug.logError(e, "PostgreSQL connection test failed", MODULE);
            return false;
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (Exception e) {
                    Debug.logError(e, "Error closing connection", MODULE);
                }
            }
        }
    }
}