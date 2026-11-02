package net.officefloor.hq.app.role;

/** The role a user name has on a sheet: clerk (any name not given one), steward, viewer (sees everything, changes nothing),
 *  or restricted (acts as a clerk, but its people list shows only the people of the crews led or joined by the person
 *  of the user's name, and that person). */
public record Role(String user, String role) {

    public static final String CLERK = "clerk";
    public static final String STEWARD = "steward";
    public static final String VIEWER = "viewer";
    public static final String RESTRICTED = "restricted";
}
