package net.officefloor.hq.app.role;

/** A role with no user name, or a role other than clerk, steward, viewer or restricted. */
public class InvalidRoleException extends Exception {
    public InvalidRoleException(String message) {
        super(message);
    }
}
