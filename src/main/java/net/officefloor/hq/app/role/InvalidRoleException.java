package net.officefloor.hq.app.role;

/** A role form that cannot be saved: a blank user name or a role other than clerk or steward. */
public class InvalidRoleException extends Exception {
    public InvalidRoleException(String message) {
        super(message);
    }
}
