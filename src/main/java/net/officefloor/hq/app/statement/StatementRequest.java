package net.officefloor.hq.app.statement;

/** A statement as the screen sends it: the date it is taken as of (blank: now). */
public record StatementRequest(String asOf) {
}
