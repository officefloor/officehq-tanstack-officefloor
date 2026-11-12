package net.officefloor.hq.app.claim;

/** Body of an OPEN claim's deferral, as sent: the date (yyyy-MM-dd) it is deferred until and who defers it. */
public record DeferRequest(String until, String user) {
}
