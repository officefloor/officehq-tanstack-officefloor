package net.officefloor.hq.app.voidreason;

/** A void reason on a sheet's list, added by `by`. */
public record VoidReason(String sheetId, String reason, String by) {
}
