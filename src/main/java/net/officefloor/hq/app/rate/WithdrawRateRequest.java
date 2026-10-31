package net.officefloor.hq.app.rate;

/** Body of POST /api/sheets/{sheetId}/rates/withdraw: the grade and the from-date (YYYY-MM-DD; blank for from the
 *  beginning of time) of the rate whose newest version is withdrawn, and the user withdrawing it. */
public record WithdrawRateRequest(String grade, String from, String user) {
}
