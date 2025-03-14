package org.rri.ideals.server.dependency;

import java.util.List;

/**
 * Represents a node in a dependency tree.
 * Contains information about a file and its dependencies.
 */
public class DependencyNode {
    private final String uri;
    private final List<String> dependencies;

    public DependencyNode(String uri, List<String> dependencies) {
        this.uri = uri;
        this.dependencies = dependencies;
    }

    public String getUri() {
        return uri;
    }

    public List<String> getDependencies() {
        return dependencies;
    }
}