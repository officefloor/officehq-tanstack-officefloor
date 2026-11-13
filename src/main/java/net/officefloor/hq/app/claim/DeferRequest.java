package net.officefloor.hq.app.claim;

/** What a deferral submits: the date (yyyy-MM-dd) the claim is deferred until (null or blank: none typed). */
public record DeferRequest(String until) {
}
