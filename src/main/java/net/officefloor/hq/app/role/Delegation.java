package net.officefloor.hq.app.role;

/** The steward role delegated to a user by a steward, for a from- and to-date (yyyy-MM-dd), both inclusive. */
public record Delegation(long id, String sheetId, String user, String steward, String from, String to) {
}
