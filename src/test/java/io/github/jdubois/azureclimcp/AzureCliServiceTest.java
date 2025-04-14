package io.github.jdubois.azureclimcp;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class AzureCliServiceTest {

    @Test
    void testHandleAzLoginCommand() throws Exception {
        // Arrange
        AzureCliService azureCliService = Mockito.spy(new AzureCliService(""));
        String command = "az login";

        // Mock the process behavior
        Process mockProcess = mock(Process.class);
        String mockOutput = "To sign in, use a web browser to open the page https://microsoft.com/devicelogin and enter the code ERAL5J27G to authenticate.\n";
        InputStream mockInputStream = new ByteArrayInputStream(mockOutput.getBytes());

        doReturn(mockInputStream).when(mockProcess).getInputStream();
        doReturn(0).when(mockProcess).waitFor();

        ProcessBuilder mockProcessBuilder = mock(ProcessBuilder.class);
        doReturn(mockProcess).when(mockProcessBuilder).start();

        // Mock the ProcessBuilder creation
        doReturn(mockProcessBuilder).when(azureCliService).createProcessBuilder(anyString());

        // Act
        String result = azureCliService.handleAzLoginCommand(command);

        // verify that azureCliService.handleAzAuthSuccess() is called
        verify(azureCliService, times(1)).handleAzAuthSuccess();
        // verify that azureCliService.handleAzAuthFailure() is not called
        verify(azureCliService, never()).handleAzAuthFailure(any());

        // Assert
        assertEquals(
            "To sign in, use a web browser to open the page https://microsoft.com/devicelogin and enter the code ERAL5J27G to authenticate.",
            result
        );
    }
}
