package net.officefloor.hq.app.role;

/** The user name is blank, or the role is not clerk or steward. */
public class InvalidRoleException extends Exception {
    public InvalidRoleException(String message) {
        super(message);
    }
}
