package org.rri.ideals.server.dependency;

import java.util.List;

/**
 * Represents a dependency tree for a file.
 * Contains information about direct and indirect dependencies.
 */
public class DependencyTree {
    private final String rootUri;
    private final List<DependencyNode> dependencies;

    public DependencyTree(String rootUri, List<DependencyNode> dependencies) {
        this.rootUri = rootUri;
        this.dependencies = dependencies;
    }

    public String getRootUri() {
        return rootUri;
    }

    public List<DependencyNode> getDependencies() {
        return dependencies;
    }
}