package net.officefloor.hq.app.delegation;

/** A delegation with no signed-in user, no user name, bad dates, a to-date before the from-date, or made by anyone
 *  but a steward. */
public class InvalidDelegationException extends Exception {
    public InvalidDelegationException(String message) {
        super(message);
    }
}
