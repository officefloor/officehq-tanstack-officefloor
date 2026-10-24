package net.officefloor.hq.app.role;

/** A role form that cannot be saved: a blank user name or a role other than clerk, steward or viewer. */
public class InvalidRoleException extends Exception {
    public InvalidRoleException(String message) {
        super(message);
    }
}
