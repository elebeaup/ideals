package org.rri.ideals.server.dependency;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Provides command handlers for dependency analysis commands.
 */
public class DependencyCommands {
    private static final Logger LOG = Logger.getInstance(DependencyCommands.class);
    private final DependencyAnalyzer analyzer;
    private final Gson gson = new Gson();

    public DependencyCommands(Project project) {
        this.analyzer = new DependencyAnalyzer(project);
    }

    /**
     * Analyzes dependencies for a file.
     *
     * @param arguments Command arguments, must contain a URI
     * @return A JSON representation of the dependency tree
     */
    public CompletableFuture<Object> analyzeDependencies(List<Object> arguments) {
        String uri = extractUri(arguments);
        if (uri == null) {
            LOG.warn("analyzeDependencies called with missing URI");
            return CompletableFuture.completedFuture("Error: Missing file URI");
        }

        LOG.info("Analyzing dependencies for URI: " + uri);
        return analyzer.getForwardDependencyTree(uri)
                .thenApply(tree -> {
                    LOG.info("Dependency analysis complete. Found " + 
                            (tree.getDependencies() != null ? tree.getDependencies().size() : 0) + 
                            " dependencies for " + uri);
                    return gson.toJson(tree);
                });
    }

    /**
     * Generates a visualization of dependencies for a file.
     *
     * @param arguments Command arguments, must contain a URI
     * @return A JSON representation of the dependency visualization
     */
    public CompletableFuture<Object> visualizeDependencies(List<Object> arguments) {
        String uri = extractUri(arguments);
        boolean includeIndirect = extractIncludeIndirect(arguments);
        
        if (uri == null) {
            LOG.warn("visualizeDependencies called with missing URI");
            return CompletableFuture.completedFuture("Error: Missing file URI");
        }

        LOG.info("Visualizing dependencies for URI: " + uri + " (includeIndirect=" + includeIndirect + ")");
        return analyzer.getForwardDependencyTree(uri)
                .thenApply(tree -> {
                    JsonObject result = new JsonObject();
                    result.addProperty("rootUri", tree.getRootUri());
                    result.add("dependencies", gson.toJsonTree(tree.getDependencies()));
                    result.addProperty("includeIndirect", includeIndirect);
                    
                    LOG.info("Dependency visualization complete. Found " + 
                            (tree.getDependencies() != null ? tree.getDependencies().size() : 0) + 
                            " dependencies for " + uri);
                    return result;
                });
    }

    /**
     * Analyzes the impact of changing a file.
     *
     * @param arguments Command arguments, must contain a URI
     * @return A JSON representation of the impact analysis
     */
    public CompletableFuture<Object> analyzeImpact(List<Object> arguments) {
        String uri = extractUri(arguments);
        boolean showTree = extractShowTree(arguments);
        
        if (uri == null) {
            LOG.warn("analyzeImpact called with missing URI");
            return CompletableFuture.completedFuture("Error: Missing file URI");
        }

        LOG.info("Analyzing impact for URI: " + uri + " (showTree=" + showTree + ")");
        return analyzer.analyzeImpact(uri)
                .thenApply(impact -> {
                    JsonObject result = new JsonObject();
                    result.addProperty("sourceUri", impact.getSourceUri());
                    result.add("directImpact", gson.toJsonTree(impact.getDirectImpact()));
                    result.add("indirectImpact", gson.toJsonTree(impact.getIndirectImpact()));
                    result.addProperty("showTree", showTree);
                    
                    LOG.info("Impact analysis complete. Found " + 
                            impact.getDirectImpact().size() + " direct and " + 
                            impact.getIndirectImpact().size() + " indirect impacts for " + uri);
                    return result;
                });
    }

    /**
     * Gets the dependency graph for the entire project.
     *
     * @param arguments Command arguments (not used)
     * @return A JSON representation of the project dependency graph
     */
    public CompletableFuture<Object> getProjectDependencyGraph(List<Object> arguments) {
        LOG.info("Getting project dependency graph");
        return analyzer.getProjectDependencyGraph()
                .thenApply(graph -> {
                    LOG.info("Project dependency graph complete. Found relationships for " + graph.size() + " files.");
                    return gson.toJson(graph);
                });
    }

    /**
     * Gets the project structure including all modules and their dependencies.
     *
     * @param arguments Command arguments (not used)
     * @return A JSON representation of the project structure
     */
    public CompletableFuture<Object> getProjectStructure(List<Object> arguments) {
        LOG.info("Getting project structure");
        // This would be implemented to return a hierarchical view of the project
        // including modules, packages, and major classes
        JsonObject result = new JsonObject();
        result.addProperty("status", "Not yet implemented");
        return CompletableFuture.completedFuture(result);
    }

    /**
     * Extracts URI from command arguments.
     * @param arguments List of command arguments
     * @return The URI string or null if not found
     */
    @Nullable
    public String extractUri(List<Object> arguments) {
        if (arguments == null || arguments.isEmpty()) {
            LOG.warn("No arguments provided to command");
            return null;
        }

        try {
            Object arg = arguments.get(0);
            LOG.info("Extracting URI from argument type: " + (arg != null ? arg.getClass().getName() : "null"));
            
            if (arg instanceof String) {
                return (String) arg;
            } else if (arg instanceof Map) {
                Map<?, ?> map = (Map<?, ?>) arg;
                Object uri = map.get("uri");
                LOG.info("Found URI in map: " + (uri != null ? uri.toString() : "null"));
                return uri != null ? uri.toString() : null;
            } else {
                // Try to parse as JSON
                String jsonString = gson.toJson(arg);
                LOG.info("Converting to JSON: " + jsonString);
                JsonObject jsonArg = gson.fromJson(jsonString, JsonObject.class);
                if (jsonArg != null && jsonArg.has("uri")) {
                    String uri = jsonArg.get("uri").getAsString();
                    LOG.info("Found URI in JSON: " + uri);
                    return uri;
                }
            }
        } catch (Exception e) {
            LOG.warn("Error extracting URI from arguments", e);
        }

        return null;
    }

    private boolean extractIncludeIndirect(List<Object> arguments) {
        if (arguments == null || arguments.isEmpty()) {
            return false;
        }

        try {
            Object arg = arguments.get(0);
            if (arg instanceof Map) {
                Map<?, ?> map = (Map<?, ?>) arg;
                Object includeIndirect = map.get("includeIndirect");
                return includeIndirect != null && Boolean.parseBoolean(includeIndirect.toString());
            } else {
                // Try to parse as JSON
                JsonObject jsonArg = gson.fromJson(gson.toJson(arg), JsonObject.class);
                if (jsonArg != null && jsonArg.has("includeIndirect")) {
                    return jsonArg.get("includeIndirect").getAsBoolean();
                }
            }
        } catch (Exception e) {
            LOG.warn("Error extracting includeIndirect flag from arguments", e);
        }

        return false;
    }

    private boolean extractShowTree(List<Object> arguments) {
        if (arguments == null || arguments.isEmpty()) {
            return false;
        }

        try {
            Object arg = arguments.get(0);
            if (arg instanceof Map) {
                Map<?, ?> map = (Map<?, ?>) arg;
                Object showTree = map.get("showTree");
                return showTree != null && Boolean.parseBoolean(showTree.toString());
            } else {
                // Try to parse as JSON
                JsonObject jsonArg = gson.fromJson(gson.toJson(arg), JsonObject.class);
                if (jsonArg != null && jsonArg.has("showTree")) {
                    return jsonArg.get("showTree").getAsBoolean();
                }
            }
        } catch (Exception e) {
            LOG.warn("Error extracting showTree flag from arguments", e);
        }

        return false;
    }
}