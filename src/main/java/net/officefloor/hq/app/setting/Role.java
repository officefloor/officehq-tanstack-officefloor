package net.officefloor.hq.app.setting;

/** A user's role on a sheet: a clerk (any name not given another role) settles only within the clerk limit; a steward settles anything. */
public enum Role {
    CLERK, STEWARD;

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
