# Azure CLI MCP Server

The Azure CLI MCP Server is an [MCP Server](https://modelcontextprotocol.io) that wraps the [Azure CLI](https://learn.microsoft.com/en-us/cli/azure/), adds a nice prompt to improve how it works, and exposes it.

> [!IMPORTANT]
> The @Azure organization offers an official Azure MCP server, which uses the code from this Azure CLI MCP server. As it's an official server, maintained by Microsoft, we recommend to use it unless you have specific resource contraints (the Azure CLI MCP Server uses less resources as it does less, and as it can be packaged with GraalVM), or if you're a Java developer and want to tweak the prompts. You can find more details at its [Getting started documentation](https://learn.microsoft.com/en-us/azure/developer/azure-mcp-server/get-started) or at the [Azure MCP Server repository](https://github.com/Azure/azure-mcp).

## Demos

### Short 2-minute demo with Claude Desktop

[![Short Demo](https://img.youtube.com/vi/y_OexCcfhW0/0.jpg)](https://www.youtube.com/watch?v=y_OexCcfhW0)

### Complete 18-minute demo with VS Code

[![Complete Demo](https://img.youtube.com/vi/NZxTr32A9lY/0.jpg)](https://www.youtube.com/watch?v=NZxTr32A9lY)

## What can it do?

It has access to the full Azure CLI, so it can do anything the Azure CLI can do. Here are a few scenarios:

- Listing your resources and checking their configuration. For example, you can get the rate limits of a model deployed
  to Azure OpenAI.
- Fixing some configuration or security issues. For example, you can ask it to secure a Blob Storage account.
- Creating resources. For example, you can ask it to create an Azure Container Apps instance, an Azure Container Registry, and connect them using managed identity.

## Is it safe to use?

As the MCP server is driven by an LLM, we would recommend to be careful and validate the commands it generates. Then, if
you're using a good LLM like Claude 4 or GPT-4o, which has
excellent training data on Azure, our experience has been very good.

Please read our [License](LICENSE) which states that "THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND",
so you use this MCP server at your own risk.

## Is it secured, and should I run this on a remote server?

Short answer: **NO**.

This MCP server runs `az` commands for you, and could be hacked by an attacker to run any other command. It's supposed
to run locally on your machine,
using your Azure CLI credentials, as you would do by yourself.

This server uses the `http` transport, and an Azure token authentication, so that it could be used remotely by different
persons. It's probably not a good idea to do this, as it would expose your Azure resources to the internet.

## How do I install it?

_This server can run inside a Docker container or as a Java executable JAR file._

By default, the server runs on port `8085`, and this can be configured using the usual Spring Boot configuration options
for configuring the server port (for example, using the `SERVER_PORT=8085` environment variable).

### Install and configure the server with Docker

Create an Azure Service Principal and set the `AZURE_CREDENTIALS` environment variable. You can do this by running the
following command in your terminal:

```bash
az ad sp create-for-rbac --name "azure-cli-mcp" --role contributor --scopes /subscriptions/<your-subscription-id>/resourceGroups/<your-resource-group> --json-auth
```

This will create a new Service Principal with the specified name and role, and output the credentials in JSON format.

You can then run the server using Docker with the following command. To authenticate, set the `AZURE_CREDENTIALS` with
the output of the previous command.

```bash
docker run --rm -p 8085:8085 -e AZURE_CREDENTIALS="{"clientId":"....","clientSecret":"....",...}" -it ghcr.io/jdubois/azure-cli-mcp:latest
```

### Install and configure the server with Java

This configuration is running the server locally. It's easier to set up than with Docker,
but it's less secured as it uses directly your credentials using the Azure CLI configured on your machine.

- Install the Azure CLI: you can do this by following the
  instructions [here](https://learn.microsoft.com/en-us/cli/azure/install-azure-cli).
- Authenticate to your Azure account. You can do this by running `az login` in your terminal.
- Make sure you have Java 17 or higher installed. You can check this by running `java -version` in your terminal.

Binaries are available on the [GitHub Release page](https://github.com/jdubois/azure-cli-mcp/releases), here's how you
can download the latest one with the GitHub CLI:

- Download the latest release: `gh release download --repo jdubois/azure-cli-mcp --pattern='azure-cli-mcp.jar'`

This MCP server is a Spring Boot application, which can be run using the classical Spring Boot commands, for example
with Maven:

```bash
mvn spring-boot:run
```

## Using the MCP Server from VS Code

To use the server from VS Code:

- Install GitHub Copilot
- Install this MCP Server using the command palette: `MCP: Add Server...`
    - The configuration connects to the server using the `http` transport
    - You need to have the server running as described above.
- Configure GitHub Copilot to run in `Agent` mode, by clicking on the arrow at the bottom of the the chat window
- On top of the chat window, you should see the `azure-cli-mcp` server configured as a tool

Here is a configuration example:

```json
{
  "servers": {
    "azure-cli-http": {
      "url": "http://localhost:8085/sse",
      "type": "http"
    }
  },
  "inputs": []
}
```
