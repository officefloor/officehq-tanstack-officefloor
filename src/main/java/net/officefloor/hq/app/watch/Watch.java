package net.officefloor.hq.app.watch;

/** That {@code user} watches the person {@code personId} on the sheet. */
public record Watch(long personId, String user) {
}
