package net.officefloor.hq.app.plan;

/** A plan that may not be added: no signed-in user, a bad amount, date or interval, or an end before the start. */
public class InvalidPlanException extends Exception {
    public InvalidPlanException(String message) {
        super(message);
    }
}
