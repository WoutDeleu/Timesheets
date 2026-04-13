package io.timesheets.model;

public enum WorkLocation {
    HOME("Thuis"),
    OFFICE("Kantoor");

    private final String displayName;

    WorkLocation(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
