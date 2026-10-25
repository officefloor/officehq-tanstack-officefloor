package net.officefloor.hq.app.plan;

/** No plan with that id on the sheet. */
public class PlanNotFoundException extends Exception {
    public PlanNotFoundException(String planId) {
        super("No plan " + planId + ".");
    }
}
