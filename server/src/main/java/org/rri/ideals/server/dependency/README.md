# Dependency Analysis for IntelliJ LSP Server

This module adds dependency analysis capabilities to the IntelliJ LSP server, allowing clients to:

1. Analyze dependencies of a specific file
2. Visualize dependency relationships
3. Analyze the impact of changing a file
4. Get a project-wide dependency graph

## Supported Commands

The following LSP commands are supported:

- `repo-prompt.analyzeDependencies` - Analyzes dependencies for a file
- `repo-prompt.visualizeDependencies` - Generates a visualization of dependencies for a file
- `repo-prompt.analyzeImpact` - Analyzes the impact of changing a file
- `repo-prompt.getProjectStructure` - Gets the project structure including modules and dependencies

## Usage with IntelliJ IDEA

To use these commands from an IntelliJ-based LSP client:

1. Send a `workspace/executeCommand` request with one of the above commands
2. Include a parameter object with a `uri` field that points to the file to analyze

Example command:

```json
{
  "command": "repo-prompt.analyzeDependencies",
  "arguments": [
    {
      "uri": "file:///path/to/your/file.java"
    }
  ]
}
```

## Implementation Details

The implementation includes:

- `DependencyAnalyzer.java` - Core analysis logic that traverses PSI structures
- `DependencyCommands.java` - Command handlers that interpret LSP requests
- `MyWorkspaceService.java` - Workspace service implementation that dispatches commands
- Support model classes (`DependencyTree`, `DependencyNode`, `DependencyImpact`)

The analysis works by:
1. Resolving file URIs to PSI files
2. Finding references between files using IntelliJ's reference search capabilities
3. Building dependency trees and graphs based on these references
4. Returning results in JSON format

## Limitations

- Currently optimized for Java projects
- Reference search performance may vary with large codebases
- The dependency graph is built on-demand and is not persisted
