package net.officefloor.hq.app.claim;

/** The signed-in user (`by`) waiving a claim's late charge. */
public record WaiveRequest(String by) {
}
