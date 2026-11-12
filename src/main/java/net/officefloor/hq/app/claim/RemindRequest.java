package net.officefloor.hq.app.claim;

/** Body of a claim's reminder, as sent: who reminds it. */
public record RemindRequest(String user) {
}
