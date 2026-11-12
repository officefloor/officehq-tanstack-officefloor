package net.officefloor.hq.app.plan;

/** Body of a pause (on true) or resume (on false) of a plan, and who makes it. */
public record PlanPauseRequest(Boolean on, String user) {
}
