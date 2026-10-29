package net.officefloor.hq.app.plan;

/** The body of a pause: {@code paused} true to pause the plan, false to resume it. */
public record PlanPauseRequest(boolean paused) {
}
