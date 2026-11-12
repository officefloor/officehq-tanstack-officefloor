package net.officefloor.hq.app.claim;

/** Body of an OPEN claim's split, as sent: the amount to split off it and who splits it. */
public record SplitRequest(String amount, String user) {
}
