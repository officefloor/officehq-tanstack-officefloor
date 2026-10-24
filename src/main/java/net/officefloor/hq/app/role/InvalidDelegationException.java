package net.officefloor.hq.app.role;

/** A delegation that cannot be made: a blank user name, a date that is not one, a to-date before the from-date, or
 *  one asked for by anyone but a steward. */
public class InvalidDelegationException extends Exception {
    public InvalidDelegationException(String message) {
        super(message);
    }
}
