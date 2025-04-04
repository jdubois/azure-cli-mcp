# Build stage for native image
FROM ghcr.io/graalvm/native-image:latest AS build

# Install Maven
RUN microdnf install -y maven

WORKDIR /app
COPY pom.xml .
# Download dependencies separately to leverage Docker caching
RUN mvn dependency:go-offline -B
COPY src src
# Build native image
RUN mvn -Pnative native:compile -DskipTests

# Runtime stage
FROM ubuntu:22.04

# Install Azure CLI
RUN apt-get update && \
    apt-get install -y ca-certificates curl apt-transport-https lsb-release gnupg && \
    mkdir -p /etc/apt/keyrings && \
    curl -sLS https://packages.microsoft.com/keys/microsoft.asc | \
    gpg --dearmor | \
    tee /etc/apt/keyrings/microsoft.gpg > /dev/null && \
    echo "deb [arch=amd64 signed-by=/etc/apt/keyrings/microsoft.gpg] https://packages.microsoft.com/repos/azure-cli/ $(lsb_release -cs) main" | \
    tee /etc/apt/sources.list.d/azure-cli.list && \
    apt-get update && \
    apt-get install -y azure-cli && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY --from=build /app/target/azure-cli-mcp .

# Create a non-root user
RUN useradd -m appuser
USER appuser

# Set environment variables to enable Azure CLI MCP with STDIO transport
ENV SPRING_MAIN_BANNER_MODE=off
ENV LOGGING_PATTERN_CONSOLE=""
ENV LOGGING_FILE_NAME=/tmp/azure-cli-mcp.log

EXPOSE 6273

ENTRYPOINT ["/app/azure-cli-mcp"]
