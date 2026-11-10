package net.officefloor.hq.app.statement;

/** A statement just taken: the person's due-total then (n/a when none) and how many claims counted. */
public record Statement(String dueTotal, int claims) {
}
