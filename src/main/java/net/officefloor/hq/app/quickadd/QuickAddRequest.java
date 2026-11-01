package net.officefloor.hq.app.quickadd;

/** Body of POST /api/sheets/{sheet}/quick-add: the person's name and the claim's amount. */
public record QuickAddRequest(String name, String amount) {
}
