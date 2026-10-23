package net.officefloor.hq.app.role;

/** A role assignment with no user name, a role that is not clerk, steward, viewer or restricted, or no signed-in user. */
public class InvalidRoleException extends Exception {
    public InvalidRoleException(String message) {
        super(message);
    }
}
