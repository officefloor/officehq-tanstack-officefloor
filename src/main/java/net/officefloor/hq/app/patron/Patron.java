package net.officefloor.hq.app.patron;

/** That the person {@code personId} has named {@code patronId}, another person on the sheet, as a patron. */
public record Patron(long personId, long patronId) {
}
