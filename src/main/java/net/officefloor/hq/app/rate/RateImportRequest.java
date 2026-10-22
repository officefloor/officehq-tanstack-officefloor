package net.officefloor.hq.app.rate;

/** Body of a rate import: who is making it, and the pasted lines grade,rate,from,to. */
public record RateImportRequest(String user, String text) {
}
