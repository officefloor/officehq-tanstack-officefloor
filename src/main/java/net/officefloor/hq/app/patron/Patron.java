package net.officefloor.hq.app.patron;

/** That the person {@code personId} names {@code patronId}, another person on the same sheet, as a patron. */
public record Patron(String sheetId, long personId, long patronId) {
}
