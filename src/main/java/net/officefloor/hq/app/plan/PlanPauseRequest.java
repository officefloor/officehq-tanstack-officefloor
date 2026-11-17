package net.officefloor.hq.app.plan;

/** Body of pausing or resuming a plan: who does it. */
public record PlanPauseRequest(String by) {
}
