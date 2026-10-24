package net.officefloor.hq.app.quickadd;

/** The quick-add bar as submitted: the name of the person and the amount of their new draft claim. */
public record QuickAddRequest(String name, String amount) {
}
