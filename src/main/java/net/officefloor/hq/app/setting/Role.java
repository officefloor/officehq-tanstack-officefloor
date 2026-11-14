package net.officefloor.hq.app.setting;

/**
 * A user's role on a sheet: a clerk (any name not given another role) settles only within the clerk limit; a steward
 * settles anything; a viewer sees everything and changes nothing; a restricted user acts as a clerk but sees on the
 * people list only the people of the crews they lead or belong to (by a person with their name) and that person.
 */
public enum Role {
    CLERK, STEWARD, VIEWER, RESTRICTED;

    /** The role as it is typed and shown: lower case. */
    public String label() {
        return name().toLowerCase();
    }

    /** The role with this label (case and outer spaces ignored), or null. */
    public static Role of(String label) {
        String wanted = label == null ? "" : label.trim();
        for (Role role : values()) {
            if (role.label().equalsIgnoreCase(wanted)) {
                return role;
            }
        }
        return null;
    }
}
