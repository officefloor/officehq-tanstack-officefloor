package net.officefloor.hq.app.voidreason;

/** A reason on a sheet's list of void reasons (trimmed), and who added it. */
public record VoidReason(String sheetId, String reason, String by) {
}
