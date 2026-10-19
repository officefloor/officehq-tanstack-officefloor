package net.officefloor.hq.app.claim;

/** The quick-add bar: the request body to add a DRAFT claim for a person named on the sheet. */
public record QuickAddRequest(String name, String amount) {
}
