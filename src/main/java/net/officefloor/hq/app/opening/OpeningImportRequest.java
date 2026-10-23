package net.officefloor.hq.app.opening;

/** Body of an opening-balance paste: who is making it, and the pasted lines name,period,amount. */
public record OpeningImportRequest(String user, String text) {
}
