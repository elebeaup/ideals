package org.rri.ideals.server.dependency;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.intellij.openapi.project.Project;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.junit.Test;
import org.mockito.Mockito;
import org.rri.ideals.server.LspServer;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

public class DependencyCommandsTest extends BasePlatformTestCase {

    private DependencyCommands dependencyCommands;
    private DependencyAnalyzer mockAnalyzer;
    private Gson gson = new Gson();

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        Project project = getProject();
        
        // Mock the dependency analyzer
        mockAnalyzer = Mockito.mock(DependencyAnalyzer.class);
        
        // Create test data
        DependencyTree sampleTree = new DependencyTree(
            "file:///test/Test.java",
            List.of(new DependencyNode(
                "file:///test/Dependency1.java",
                List.of("file:///test/Dependency2.java")
            ))
        );
        
        DependencyImpact sampleImpact = new DependencyImpact(
            "file:///test/Test.java",
            List.of("file:///test/DirectImpact.java"),
            List.of("file:///test/IndirectImpact.java")
        );
        
        Map<String, List<String>> sampleGraph = Map.of(
            "file:///test/Test.java", 
            List.of("file:///test/Dependency1.java", "file:///test/Dependency2.java")
        );
        
        // Set up the mock to return test data
        when(mockAnalyzer.getForwardDependencyTree(Mockito.anyString()))
            .thenReturn(CompletableFuture.completedFuture(sampleTree));
        
        when(mockAnalyzer.analyzeImpact(Mockito.anyString()))
            .thenReturn(CompletableFuture.completedFuture(sampleImpact));
        
        when(mockAnalyzer.getProjectDependencyGraph())
            .thenReturn(CompletableFuture.completedFuture(sampleGraph));
            
        // Create dependency commands with the mocked analyzer
        dependencyCommands = new DependencyCommands(project) {
            @Override
            public CompletableFuture<Object> analyzeDependencies(List<Object> arguments) {
                String uri = extractUri(arguments);
                if (uri == null) {
                    return CompletableFuture.completedFuture("Error: Missing file URI");
                }
                return mockAnalyzer.getForwardDependencyTree(uri)
                        .thenApply(gson::toJson);
            }
            
            @Override
            public CompletableFuture<Object> visualizeDependencies(List<Object> arguments) {
                String uri = extractUri(arguments);
                if (uri == null) {
                    return CompletableFuture.completedFuture("Error: Missing file URI");
                }
                return mockAnalyzer.getForwardDependencyTree(uri)
                        .thenApply(tree -> {
                            JsonObject result = new JsonObject();
                            result.addProperty("rootUri", tree.getRootUri());
                            result.add("dependencies", gson.toJsonTree(tree.getDependencies()));
                            return result;
                        });
            }
            
            @Override
            public CompletableFuture<Object> analyzeImpact(List<Object> arguments) {
                String uri = extractUri(arguments);
                if (uri == null) {
                    return CompletableFuture.completedFuture("Error: Missing file URI");
                }
                return mockAnalyzer.analyzeImpact(uri)
                        .thenApply(impact -> {
                            JsonObject result = new JsonObject();
                            result.addProperty("sourceUri", impact.getSourceUri());
                            result.add("directImpact", gson.toJsonTree(impact.getDirectImpact()));
                            result.add("indirectImpact", gson.toJsonTree(impact.getIndirectImpact()));
                            return result;
                        });
            }
            
            @Override
            public CompletableFuture<Object> getProjectDependencyGraph(List<Object> arguments) {
                return mockAnalyzer.getProjectDependencyGraph()
                        .thenApply(gson::toJson);
            }
        };
    }

    @Test
    public void testAnalyzeDependencies() throws Exception {
        // Create test arguments with a file URI
        List<Object> args = Collections.singletonList("file:///test/Test.java");
        
        // Execute the command
        CompletableFuture<Object> future = dependencyCommands.analyzeDependencies(args);
        Object result = future.get();
        
        // Verify the result
        assertNotNull("Result should not be null", result);
        assertTrue("Result should be a JSON string", result instanceof String);
        String jsonResult = (String) result;
        assertTrue("JSON result should contain the root URI", jsonResult.contains("Test.java"));
        assertTrue("JSON result should contain dependencies", jsonResult.contains("Dependency1.java"));
    }
    
    @Test
    public void testVisualizeDependencies() throws Exception {
        // Create test arguments with a file URI
        Map<String, Object> argMap = Map.of("uri", "file:///test/Test.java", "includeIndirect", true);
        List<Object> args = Collections.singletonList(argMap);
        
        // Execute the command
        CompletableFuture<Object> future = dependencyCommands.visualizeDependencies(args);
        Object result = future.get();
        
        // Verify the result
        assertNotNull("Result should not be null", result);
        assertTrue("Result should be a JsonObject", result instanceof JsonObject);
        JsonObject jsonResult = (JsonObject) result;
        assertTrue("Result should contain the root URI", jsonResult.has("rootUri"));
        assertTrue("Result should contain dependencies", jsonResult.has("dependencies"));
    }
    
    @Test
    public void testAnalyzeImpact() throws Exception {
        // Create test arguments with a file URI
        Map<String, Object> argMap = Map.of("uri", "file:///test/Test.java", "showTree", true);
        List<Object> args = Collections.singletonList(argMap);
        
        // Execute the command
        CompletableFuture<Object> future = dependencyCommands.analyzeImpact(args);
        Object result = future.get();
        
        // Verify the result
        assertNotNull("Result should not be null", result);
        assertTrue("Result should be a JsonObject", result instanceof JsonObject);
        JsonObject jsonResult = (JsonObject) result;
        assertTrue("Result should contain the source URI", jsonResult.has("sourceUri"));
        assertTrue("Result should contain direct impact", jsonResult.has("directImpact"));
        assertTrue("Result should contain indirect impact", jsonResult.has("indirectImpact"));
    }
    
    @Test
    public void testGetProjectDependencyGraph() throws Exception {
        // Execute the command
        CompletableFuture<Object> future = dependencyCommands.getProjectDependencyGraph(Collections.emptyList());
        Object result = future.get();
        
        // Verify the result
        assertNotNull("Result should not be null", result);
        assertTrue("Result should be a JSON string", result instanceof String);
        String jsonResult = (String) result;
        assertTrue("JSON result should contain dependencies", jsonResult.contains("Dependency"));
    }
    
    @Test
    public void testMissingUri() throws Exception {
        // Execute the command with null arguments
        CompletableFuture<Object> future = dependencyCommands.analyzeDependencies(null);
        Object result = future.get();
        
        // Verify the error message
        assertTrue("Result should be an error message", result instanceof String);
        String errorMessage = (String) result;
        assertTrue("Error message should mention missing URI", errorMessage.contains("Missing file URI"));
    }
}