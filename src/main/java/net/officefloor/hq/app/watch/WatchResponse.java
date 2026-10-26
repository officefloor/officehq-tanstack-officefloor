package net.officefloor.hq.app.watch;

/** One watch: the signed-in user ({@code user}) watches the person ({@code personId}). */
public record WatchResponse(long personId, String user) {
}
