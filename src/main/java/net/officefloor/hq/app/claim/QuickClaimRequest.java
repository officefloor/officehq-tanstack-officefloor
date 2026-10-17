package net.officefloor.hq.app.claim;

/** The body of the quick-add bar: a person's name (required; created with grade A when not on the
 *  sheet) and an amount (required). The claim takes the sheet's today and an empty note. */
public record QuickClaimRequest(String name, String amount) {
}
