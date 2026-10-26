package net.officefloor.hq.app.plan;

/** The signed-in user (`by`) generating the plans' claims. */
public record GenerateRequest(String by) {
}
