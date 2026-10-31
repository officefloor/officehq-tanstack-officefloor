package net.officefloor.hq.app.quickadd;

/** Body of a quick-add, as typed: the person's name, the claim's amount and who makes the change. */
public record QuickAddRequest(String name, String amount, String user) {
}
