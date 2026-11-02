package net.officefloor.hq.app.role;

/** The role's user name is blank, or its role is not clerk, steward, viewer or restricted. */
public class InvalidRoleException extends Exception {
    public InvalidRoleException(String message) {
        super(message);
    }
}
