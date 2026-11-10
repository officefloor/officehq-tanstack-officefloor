package net.officefloor.hq.app.delegation;

import java.time.LocalDate;

/**
 * The steward role delegated by a steward ({@code by}) to a user from a date to a date, inclusive, by the
 * sheet's today: while today is inside the range the user acts as a steward.
 */
public record Delegation(long id, String sheetId, String user, String by, LocalDate from, LocalDate to) {
}
