package net.officefloor.hq.app.plan;

/** A plan that may not be added: an amount the claim form would refuse, an interval that is not a whole number of at least 1, a day of the month not 1 to 31, a bad date or an end before the start. */
public class PlanRefusedException extends Exception {

    public PlanRefusedException(String message) {
        super(message);
    }
}
