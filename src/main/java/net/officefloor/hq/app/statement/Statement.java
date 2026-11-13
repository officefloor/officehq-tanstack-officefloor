package net.officefloor.hq.app.statement;

/** A statement just taken of a person: their due-total at that moment (n/a with none) and how many claims counted. */
public record Statement(String name, String dueTotal, int claims) {
}
