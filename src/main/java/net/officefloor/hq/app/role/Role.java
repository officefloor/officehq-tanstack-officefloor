package net.officefloor.hq.app.role;

/** The role a user name has on a sheet: clerk (any name not given one) or steward. */
public record Role(String user, String role) {

    public static final String CLERK = "clerk";
    public static final String STEWARD = "steward";
}
