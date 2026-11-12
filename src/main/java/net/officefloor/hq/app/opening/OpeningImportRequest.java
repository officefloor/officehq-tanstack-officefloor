package net.officefloor.hq.app.opening;

/** Body of a paste of opening balances, as typed: lines of name,period,amount and who makes the change. */
public record OpeningImportRequest(String text, String user) {
}
