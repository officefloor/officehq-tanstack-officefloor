package net.officefloor.hq.app.plan;

/** Body of a pause or resume of a plan: who presses it. */
public record PlanPauseRequest(String user) {
}
