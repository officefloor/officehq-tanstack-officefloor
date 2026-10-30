package net.officefloor.hq.app.plan;

/** A plan that cannot be added (a bad amount or interval, an end before the start); nothing is written. */
public class InvalidPlanException extends Exception {
    public InvalidPlanException(String message) {
        super(message);
    }
}
