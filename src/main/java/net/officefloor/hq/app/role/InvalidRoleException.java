package net.officefloor.hq.app.role;

/** A role that cannot be given: a blank user name, or a role other than clerk or steward. */
public class InvalidRoleException extends Exception {
    public InvalidRoleException(String message) {
        super(message);
    }
}
