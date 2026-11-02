package net.officefloor.hq.app.plan;

/** The plan's amount, dates or interval are not acceptable. */
public class InvalidPlanException extends Exception {
    public InvalidPlanException(String message) {
        super(message);
    }
}
