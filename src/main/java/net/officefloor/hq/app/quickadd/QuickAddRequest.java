package net.officefloor.hq.app.quickadd;

/** Body of a quick-add: who is making it, the person's name and the claim's amount as typed. */
public record QuickAddRequest(String user, String name, String amount) {
}
