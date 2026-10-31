package net.officefloor.hq.app.plan;

/** Body of POST /api/sheets/{sheetId}/plans/{planId}/amount: the plan's new amount and who changes it. */
public record AmountRequest(String amount, String user) {
}
