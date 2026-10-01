package net.officefloor.hq.app;

/**
 * Where a project sits in its lifecycle. A project is marked as one of these; the list and the
 * create form surface the chosen value. Stored by name on the {@code projects} table
 * (V22__project_status.sql).
 */
public enum ProjectStatus {
    ACTIVE,
    ON_HOLD,
    FINISHED;

    /** Parse a submitted status, falling back to {@link #ACTIVE} for a blank/unknown value. */
    public static ProjectStatus parse(String value) {
        if (value == null) {
            return ACTIVE;
        }
        for (ProjectStatus status : values()) {
            if (status.name().equals(value.trim())) {
                return status;
            }
        }
        return ACTIVE;
    }
}
