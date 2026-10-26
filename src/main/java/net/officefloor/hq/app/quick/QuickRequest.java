package net.officefloor.hq.app.quick;

/** The body of a quick-add, as typed: the person's name and the claim's amount. */
public record QuickRequest(String name, String amount) {
}
