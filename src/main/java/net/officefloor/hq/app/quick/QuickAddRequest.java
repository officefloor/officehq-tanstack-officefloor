package net.officefloor.hq.app.quick;

/** Body of a quick-add: the person's name and the claim's amount. */
public record QuickAddRequest(String name, String amount) {
}
