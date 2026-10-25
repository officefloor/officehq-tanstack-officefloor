package net.officefloor.hq.app.rollover;

/** A rollover that cannot go through: a year that is not yyyy, a year with no periods, or one asked for by anyone
 *  but a steward. */
public class InvalidRolloverException extends Exception {
    public InvalidRolloverException(String message) {
        super(message);
    }
}
