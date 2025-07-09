# Azure CLI MCP Server

> [!IMPORTANT]
> The @Azure organization offers an official Azure MCP server, which uses the code from this Azure CLI MCP server. As it's an official server, maintained by Microsoft, we recommend to use it unless you have specific resource contraints (the Azure CLI MCP Server uses less resources as it does less, and as it can be packaged with GraalVM), or if you're a Java developer and want to tweak the prompts. You can find more details at its [Getting started documentation](https://learn.microsoft.com/en-us/azure/developer/azure-mcp-server/get-started) or at the [Azure MCP Server repository](https://github.com/Azure/azure-mcp).

This is an [MCP Server](https://modelcontextprotocol.io) that wraps the [Azure CLI](https://learn.microsoft.com/en-us/cli/azure/), adds a nice prompt to improve how it works, and exposes it.

[![smithery badge](https://smithery.ai/badge/@jdubois/azure-cli-mcp)](https://smithery.ai/server/@jdubois/azure-cli-mcp)

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
you're using a good LLM like Claude 3.7 or GPT-4o, which has
excellent training data on Azure, our experience has been very good.

Please read our [License](LICENSE) which states that "THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND",
so you use this MCP server at your own risk.

## Is it secured, and should I run this on a remote server?

Short answer: **NO**.

This MCP server runs `az` commands for you, and could be hacked by an attacker to run any other command. The current
implementation, as with most MCP servers at the moment, only works with the `stio` transport:
it's supposed to run locally on your machine, using your Azure CLI credentials, as you would do by yourself.

In the future, it's totally possible to have this MCP server support the `http` transport, and an Azure token
authentication, so that it could be used remotely by different persons. It's a second step, that will be done once the
MCP specification and SDK are more stable.

## How do I install it?

_This server can run inside a Docker container or as a Java executable JAR file._

For both options, only the `stio` transport is available. The `http` transport will be available later.

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
docker run --rm -p 6273:6273 -e AZURE_CREDENTIALS="{"clientId":"....","clientSecret":"....",...}" -i ghcr.io/jdubois/azure-cli-mcp:latest
```

#### Using VS Code

To use the server from VS Code:

- Install GitHub Copilot
- Install this MCP Server using the command palette: `MCP: Add Server...`
  - The configuration connects to the server using the `stio` transport
  - The command to run is `docker run -i --rm -e AZURE_CREDENTIALS ghcr.io/jdubois/azure-cli-mcp:latest`. You'll need to
    set the `AZURE_CREDENTIALS` environment variable to the JSON output from the Service Principal creation, with the
    quotes escaped: have a look below for a complete and secure example.
- Configure GitHub Copilot to run in `Agent` mode, by clicking on the arrow at the bottom of the the chat window
- On top of the chat window, you should see the `azure-cli-mcp` server configured as a tool

You can secure the `AZURE_CREDENTIALS` environment using the methode
described [in the documentation](https://code.visualstudio.com/docs/copilot/chat/mcp-servers#_configuration-example),
here is a complete example:

```json
{
  "inputs": [
    {
      "type": "promptString",
      "id": "azure-credentials",
      "description": "Azure Credentials",
      "password": true
    }
  ],
  "servers": {
    "azure-cli": {
      "command": "docker",
      "args": [
        "run",
        "-i",
        "--rm",
        "-e",
        "AZURE_CREDENTIALS",
        "ghcr.io/jdubois/azure-cli-mcp:latest"
      ],
      "env": {
        "AZURE_CREDENTIALS": "${input:azure-credentials}"
      }
    }
  }
}
```

#### Using Claude Desktop

To use the server from Claude Desktop, add the server to your `claude_desktop_config.json` file.
The `AZURE_CREDENTIALS` environment variable should be set to the JSON output from the Service Principal creation, with
the quotes escaped.

```json
{
  "mcpServers": {
    "azure-cli": {
      "command": "docker",
      "args": [
        "run",
        "-i",
        "--rm",
        "-e",
        "AZURE_CREDENTIALS",
        "ghcr.io/jdubois/azure-cli-mcp:latest"
      ],
      "env": {
        "AZURE_CREDENTIALS": "{\"clientId\":\"...\",\"clientSecret\":\"...\",..."
      }
    }
  }
}
```

### Installation with Smithery.ai

You can install the MCP server through Smithery.ai:

[![smithery badge](https://smithery.ai/badge/@jdubois/azure-cli-mcp)](https://smithery.ai/server/@jdubois/azure-cli-mcp)

This is similar to our Docker container installation above, but runs on Smithery.ai's servers. While this installation
is initially the easiest, please note that:

- You will need an `AZURE_CREDENTIALS` key, as described below in the Docker installation section, and this key
  will be sent to Smithery.ai.
- Smithery.ai is a third-party service, and you need to trust them to build this MCP server for you (it uses the same
  Dockerfile as our Docker image, but isn't built by us).
- This is still an early preview service, so we can't guarantee how it will evolve.

### Install and configure the server with Java

This configuration is running the server locally. It's easier to set up than with Docker,
but it's less secured as it uses directly your credentials using the Azure CLI configured on your machine.

- Install the Azure CLI: you can do this by following the instructions [here](https://learn.microsoft.com/en-us/cli/azure/install-azure-cli).
- Authenticate to your Azure account. You can do this by running `az login` in your terminal.
- Make sure you have Java 17 or higher installed. You can check this by running `java -version` in your terminal.

Binaries are available on the [GitHub Release page](https://github.com/jdubois/azure-cli-mcp/releases), here's how you
can download the latest one with the GitHub CLI:

- Download the latest release: `gh release download --repo jdubois/azure-cli-mcp --pattern='azure-cli-mcp.jar'`

#### Using VS Code

- Install GitHub Copilot
- Install this MCP Server using the command palette: `MCP: Add Server...`
  - The configuration connects to the server using the `stio` transport
  - The command to run is `java -jar ~/Downloads/azure-cli-mcp.jar` (you need to point to the location where you
    downloaded the `azure-cli-mcp.jar` file)
- Configure GitHub Copilot to run in `Agent` mode, by clicking on the arrow at the bottom of the the chat window
- On top of the chat window, you should see the `azure-cli-mcp` server configured as a tool

#### Using Claude Desktop

To use the server from Claude Desktop, add the server to your `claude_desktop_config.json` file. Please note that you
need to point to the location where you downloaded the `azure-cli-mcp.jar` file.

```json
{
    "mcpServers": {
        "azure-cli": {
            "command": "java",
            "args": [
                "-jar",
              "~/Downloads/azure-cli-mcp.jar"
            ]
        }
    }
}
```
