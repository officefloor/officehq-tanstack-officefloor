package net.officefloor.hq.app.quick;

/** Body of a quick add, as typed: the person's name, the claim's amount and who makes the change. */
public record QuickAddRequest(String name, String amount, String user) {
}
