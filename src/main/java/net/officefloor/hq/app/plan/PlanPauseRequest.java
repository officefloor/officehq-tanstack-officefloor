package net.officefloor.hq.app.plan;

/** Body of pausing or resuming a plan: who is doing it. */
public record PlanPauseRequest(String user) {
}
