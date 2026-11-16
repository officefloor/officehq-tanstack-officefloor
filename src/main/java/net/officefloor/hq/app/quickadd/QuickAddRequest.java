package net.officefloor.hq.app.quickadd;

/** Body of a quick-add: the person's name (an existing one, or a new person of grade A), the claim's amount, and who makes it. */
public record QuickAddRequest(String name, String amount, String by) {
}
