package net.officefloor.hq.app.patron;

/** One link as the screen sees it: the person names the patron (both person ids). */
public record PatronResponse(long personId, long patronId) {
}
