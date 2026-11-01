package net.officefloor.hq.app.claim;

/** Body of POST /api/sheets/{sheet}/claims/{claim}/split: the amount split off the claim. */
public record SplitRequest(String amount) {
}
