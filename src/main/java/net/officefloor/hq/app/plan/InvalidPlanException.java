package net.officefloor.hq.app.plan;

/** A plan with an amount the claim form would refuse, a bad date or interval, or an end before its start. */
public class InvalidPlanException extends Exception {
    public InvalidPlanException(String message) {
        super(message);
    }
}
