package io.github.jdubois.azureclimcp;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AzureLoginHandlerTest {

    @Spy
    @InjectMocks
    private AzureLoginHandler azureLoginHandler;

    @Mock
    private Process process;

    @Mock
    private ProcessBuilder processBuilder;

    @BeforeEach
    public void setup() throws Exception {
        // Setup the ProcessBuilder mock
        doReturn(processBuilder).when(azureLoginHandler).createProcessBuilder(anyString());
        when(processBuilder.redirectErrorStream(anyBoolean())).thenReturn(processBuilder);
        when(processBuilder.start()).thenReturn(process);
        
        // Setup the Process mock - only stub what's needed for all tests
        //when(process.getInputStream()).thenReturn(new ByteArrayInputStream("".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    public void testHandleAzLoginCommand_WithDeviceCode() throws Exception {
        // Prepare test data
        String loginMessage = "To sign in, use a web browser to open the page https://microsoft.com/devicelogin and enter the code ABCDEFG to authenticate.";
        InputStream inputStream = new ByteArrayInputStream(loginMessage.getBytes(StandardCharsets.UTF_8));
        when(process.getInputStream()).thenReturn(inputStream);
        
        // For this test, we need these specific mocks for the background thread
        doReturn(true, false).when(process).isAlive();
        doNothing().when(azureLoginHandler).waitForAzLoginProcess();
        doNothing().when(azureLoginHandler).handleAzAuthSuccess();
        
        // Mock the process output stream for this test
        //OutputStream outputStream = mock(OutputStream.class);
        //when(process.getOutputStream()).thenReturn(outputStream);
        
        // Execute the method
        String result = azureLoginHandler.handleAzLoginCommand("az login --use-device-code");
        
        // Verify the result contains the login message
        assertEquals(loginMessage, result);
        
        // Verify the process was started
        verify(processBuilder).start();
    }

    @Test
    public void testHandleAzLoginCommand_AddsDeviceCodeFlag() throws Exception {
        // Prepare test data
        String loginMessage = "To sign in, use a web browser to open the page https://microsoft.com/devicelogin and enter the code ABCDEFG to authenticate.";
        InputStream inputStream = new ByteArrayInputStream(loginMessage.getBytes(StandardCharsets.UTF_8));
        when(process.getInputStream()).thenReturn(inputStream);
        
        // For this test, we need these specific mocks for the background thread
        doReturn(true, false).when(process).isAlive();
        doNothing().when(azureLoginHandler).waitForAzLoginProcess();
        doNothing().when(azureLoginHandler).handleAzAuthSuccess();
        
        // Mock the process output stream for this test
        OutputStream outputStream = mock(OutputStream.class);
//        when(process.getOutputStream()).thenReturn(outputStream);
        
        // Execute the method with a command that doesn't include the --use-device-code flag
        String result = azureLoginHandler.handleAzLoginCommand("az login");
        
        // Verify the process was started with the correct command
        verify(azureLoginHandler).createProcessBuilder("az login --use-device-code");
        
        // Verify the result contains the login message
        assertEquals(loginMessage, result);
    }

    @Test
    public void testHandleAzLoginCommand_InterruptsOngoingProcess() throws Exception {
        // Setup a field with reflection to simulate an ongoing process
        java.lang.reflect.Field field = AzureLoginHandler.class.getDeclaredField("currentLoginProcess");
        field.setAccessible(true);
        
        Process mockExistingProcess = mock(Process.class);
        when(mockExistingProcess.isAlive()).thenReturn(true);
        field.set(azureLoginHandler, mockExistingProcess);
        
        // Prepare test data for the new process
        String loginMessage = "To sign in, use a web browser to open the page https://microsoft.com/devicelogin and enter the code ABCDEFG to authenticate.";
        InputStream inputStream = new ByteArrayInputStream(loginMessage.getBytes(StandardCharsets.UTF_8));
        when(process.getInputStream()).thenReturn(inputStream);
        
        // For this test, we need these specific mocks for the background thread
        //doReturn(true, false).when(process).isAlive();
//        doNothing().when(azureLoginHandler).waitForAzLoginProcess();
//        doNothing().when(azureLoginHandler).handleAzAuthSuccess();
        
        // Mock the process output stream for this test
        OutputStream outputStream = mock(OutputStream.class);
        //when(process.getOutputStream()).thenReturn(outputStream);
        
        // Execute the method
        azureLoginHandler.handleAzLoginCommand("az login --use-device-code");
        
        // Verify the previous process was destroyed
        verify(mockExistingProcess).destroy();
    }

    @Test
    public void testHandleAzLoginCommand_ErrorHandling() throws Exception {
        // For this test we don't need any of the background thread mocks
        // as the process never starts
        
        // Prepare test data for error case
        when(processBuilder.start()).thenThrow(new java.io.IOException("Test exception"));
        
        // Execute the method
        String result = azureLoginHandler.handleAzLoginCommand("az login --use-device-code");
        
        // Verify the result contains the error message
        assertTrue(result.contains("Error:"));
        assertTrue(result.contains("Test exception"));
    }
}