package com.pvz.builder;

import java.util.List;

/** Every issue found in a map at once, so the editor can list them all. */
public final class ValidationResult {
    private final List<MapIssue> errors;
    private final List<MapIssue> warnings;

    ValidationResult(List<MapIssue> issues) {
        this.errors = issues.stream().filter(MapIssue::blocksSave).toList();
        this.warnings = issues.stream().filter(issue -> !issue.blocksSave()).toList();
    }

    public boolean canSave() {
        return errors.isEmpty();
    }

    /** Issues that block saving. The list cannot be modified. */
    public List<MapIssue> getErrors() {
        return errors;
    }

    /** Issues shown to the player that still allow saving. The list cannot be modified. */
    public List<MapIssue> getWarnings() {
        return warnings;
    }

    public boolean has(MapIssue issue) {
        return errors.contains(issue) || warnings.contains(issue);
    }
}
