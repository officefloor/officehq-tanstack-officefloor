package net.officefloor.hq.app.claim;

/** The amount to split off an OPEN claim, and the signed-in user (`by`) splitting it. */
public record SplitRequest(String amount, String by) {
}
