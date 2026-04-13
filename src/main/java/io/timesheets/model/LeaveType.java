package io.timesheets.model;

public enum LeaveType {
    VACATION("Verlof"),
    ADV("ADV"),
    SICK("Ziekteverlof"),
    TRAINING("Opleiding"),
    COMPENSATORY_REST("Inhaalrust"),
    UNPAID("Onbetaald verlof"),
    OTHER("Andere");

    private final String displayName;

    LeaveType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
