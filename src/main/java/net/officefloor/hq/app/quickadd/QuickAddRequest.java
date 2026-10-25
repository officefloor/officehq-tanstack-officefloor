package net.officefloor.hq.app.quickadd;

/** The quick-add bar as typed: a person's name, a claim amount, and the signed-in user (`by`) making the change. */
public record QuickAddRequest(String name, String amount, String by) {
}
