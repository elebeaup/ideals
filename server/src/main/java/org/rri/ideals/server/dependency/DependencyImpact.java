package org.rri.ideals.server.dependency;

import java.util.List;

/**
 * Represents the impact of changing a file.
 * Contains information about files that would be directly and indirectly affected.
 */
public class DependencyImpact {
    private final String sourceUri;
    private final List<String> directImpact;
    private final List<String> indirectImpact;

    public DependencyImpact(String sourceUri, List<String> directImpact, List<String> indirectImpact) {
        this.sourceUri = sourceUri;
        this.directImpact = directImpact;
        this.indirectImpact = indirectImpact;
    }

    public String getSourceUri() {
        return sourceUri;
    }

    public List<String> getDirectImpact() {
        return directImpact;
    }

    public List<String> getIndirectImpact() {
        return indirectImpact;
    }
}