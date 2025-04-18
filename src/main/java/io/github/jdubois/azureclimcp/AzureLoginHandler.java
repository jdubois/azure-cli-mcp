package io.github.jdubois.azureclimcp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

/**
 * Handles the Azure CLI login process, particularly for device code authentication.
 * This class separates the login handling logic from the main AzureCliService.
 */
@Component
public class AzureLoginHandler {

    private final Logger logger = LoggerFactory.getLogger(AzureLoginHandler.class);
    private Process currentLoginProcess;

    /**
     * Handles the special case for the 'az login' command.
     * <p>
     * If command starts with "az login", we need special handling:
     * 1. Ensure the "--use-device-code" flag is present
     * 2. Extract the URL and code for device login
     * 3. Keep the process running in the background
     *
     * @param command The Azure CLI login command.
     * @return The extracted URL and code for device login, or an error message.
     */
    public String handleAzLoginCommand(String command) {
        logger.info("Handling 'az login' command: {}", command);

        // Ensure the --use-device-code flag is present
        if (!command.contains("--use-device-code")) {
            command += " --use-device-code";
        }

        try {
            // Interrupt the previous login process if it is still running
            if (currentLoginProcess != null && currentLoginProcess.isAlive()) {
                logger.info("Interrupting previous 'az login' process.");
                currentLoginProcess.destroy();
            }

            ProcessBuilder processBuilder = createProcessBuilder(command);
            processBuilder.redirectErrorStream(true);
            currentLoginProcess = processBuilder.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(currentLoginProcess.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    logger.debug("Azure CLI output: {}", line);
                    output.append(line).append("\n");

                    // Look for the device login message
                    if (line.contains("To sign in") && line.contains("code")) {
                        logger.info("Extracted login instructions: {}", line);

                        // Start a background thread to keep the process running
                        new Thread(this::handleAzLoginBackground).start();

                        // Return the URL and code to the user
                        return line;
                    }
                }
            }

            // If no URL and code were found, return the full output
            return "Error: Unable to extract login URL and code. Output: " + output.toString();
        } catch (IOException e) {
            logger.error("Error running 'az login' command", e);
            return "Error: " + e.getMessage();
        }
    }

    private void handleAzLoginBackground() {
        Process process = currentLoginProcess;
        logger.info("Handling 'az login' process in the background.");
        try {
            // Check if the process is still waiting for input
            if (process.isAlive()) {
                try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()))) {
                    writer.write("1\n"); // Provide input '1' to the process
                    writer.flush();
                } catch (IOException e) {
                    logger.error("Error providing input to 'az login' process", e);
                }
            }

            waitForAzLoginProcess();
            handleAzAuthSuccess();
        } catch (InterruptedException e) {
            handleAzAuthFailure(e);
        }
    }

    protected void waitForAzLoginProcess() throws InterruptedException {
        currentLoginProcess.waitFor();
    }

    protected void handleAzAuthFailure(InterruptedException e) {
        logger.error("Error waiting for 'az login' process", e);
    }

    protected void handleAzAuthSuccess() {
        logger.info("'az login' process completed.");
    }

    /**
     * Creates a ProcessBuilder for the given command.
     * This method is protected to allow mocking in tests.
     *
     * @param command The command to execute.
     * @return A ProcessBuilder instance.
     */
    protected ProcessBuilder createProcessBuilder(String command) {
        return new ProcessBuilder("sh", "-c", command);
    }
}