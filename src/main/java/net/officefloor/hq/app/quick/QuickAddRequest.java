package net.officefloor.hq.app.quick;

/**
 * A quick-add as the screen sends it: the name of the person the claim is for (a name not on the
 * sheet adds them, with grade A) and the claim's amount.
 */
public record QuickAddRequest(String name, String amount) {
}
