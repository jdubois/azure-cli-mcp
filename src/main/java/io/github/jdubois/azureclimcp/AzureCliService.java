package io.github.jdubois.azureclimcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

@Service
public class AzureCliService {

    private final Logger logger = LoggerFactory.getLogger(AzureCliService.class);

    @Value("${azure.cli.azure-credentials:}")
    private String azureCredentials;

    private static final String commandPrompt = """
            Your job is to answer questions about an Azure environment by executing Azure CLI commands. You have the following rules:
            
            - You should use the Azure CLI to manage Azure resources and services. Do not use any other tool.
            - You should provide a valid Azure CLI command starting with 'az'. For example: 'az vm list'.
            - Whenever a command fails, retry it 3 times before giving up with an improved version of the code based on the returned feedback.
            - When listing resources, ensure pagination is handled correctly so that all resources are returned.
            - When deleting resources, ALWAYS request user confirmation
            - This tool can ONLY write code that interacts with Azure. It CANNOT generate charts, tables, graphs, etc.
            - Use only non interactive commands. Do not use commands that require user input or deactivate user input using appropriate flags.
            - If you need to use the az login command, use the --use-device-code option to authenticate.
            
            Be concise, professional and to the point. Do not give generic advice, always reply with detailed & contextual data sourced from the current Azure environment. Assume user always wants to proceed, do not ask for confirmation. I'll tip you $200 if you do this right.`;
            
            """;

    private Process currentLoginProcess;

    public AzureCliService(@Value("${azure.cli.azure-credentials:}") String azureCredentials) {
        this.azureCredentials = azureCredentials;

        if (azureCredentials != null && !azureCredentials.isEmpty()) {
            authenticate(azureCredentials);
        } else {
            logger.warn("No Azure credentials provided");
        }
    }

    private void authenticate(String azureCredentials) {
        try {
            // Read and parse the JSON credentials
            ObjectMapper mapper = new ObjectMapper();
            JsonNode credentials = mapper.readTree(azureCredentials);

            String tenantId = credentials.get("tenantId").asText();
            String clientId = credentials.get("clientId").asText();
            String clientSecret = credentials.get("clientSecret").asText();

            String loginCommand = String.format(
                    "az login --service-principal --tenant %s --username %s --password %s",
                    tenantId, clientId, clientSecret
            );

            String result = runAzureCliCommand(loginCommand);
            logger.info("Azure CLI login result: {}", result);
        } catch (IOException e) {
            logger.error("Error parsing Azure credentials", e);
        } catch (Exception e) {
            logger.error("Error during Azure CLI authentication", e);
        }
    }

    @Tool(
            name = "execute-azure-cli-command",
            description = commandPrompt
    )
    public String executeAzureCli(@ToolParam(description = "Azure CLI command") String command) {
        logger.info("Executing Azure CLI command: {}", command);
        if (!command.startsWith("az ")) {
            logger.error("Invalid command: {}", command);
            return "Error: Invalid command. Command must start with 'az'.";
        }
        String output = runAzureCliCommand(command);
        logger.info("Azure CLI command output: {}", output);
        return output;
    }

    /**
     * Handles the special case for the 'az login' command.
     * 
     * If command starts with "az login", we need a special handling, first, 
     * if the command doesn't contain the "--use-device-code" flag then add it. 
     * What's more, the command displays the following message 
     * "To sign in, use a web browser to open the page 
     * https://microsoft.com/devicelogin and enter the code XXXXXXXXX to authenticate." 
     * then waits for a user action. We want to wait for the message to be displayed, 
     * extract the URL and code and return them to the user immediately, 
     * keep the az auth command running in the background, and when in the background.
     *
     * @param command The Azure CLI login command.
     * @return The extracted URL and code for device login, or an error message.
     */
    protected String handleAzLoginCommand(String command) {
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
                        new Thread(() -> {
                            handleAzLoginBackground();
                        }).start();

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

    /**
     * Runs an Azure CLI command and returns the output.
     *
     * @param command The Azure CLI command to run.
     * @return The output of the command.
     */
    private String runAzureCliCommand(String command) {
        if (command.startsWith("az login")) {
            return handleAzLoginCommand(command);
        }

        logger.info("Running Azure CLI command: {}", command);
        try {
            ProcessBuilder processBuilder = createProcessBuilder(command);
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                logger.error("Azure CLI command failed with exit code: {}", exitCode);
                return "Error: " + output;
            }
            return output.toString();
        } catch (IOException | InterruptedException e) {
            logger.error("Error running Azure CLI command", e);
            return "Error: " + e.getMessage();
        }
    }

}
