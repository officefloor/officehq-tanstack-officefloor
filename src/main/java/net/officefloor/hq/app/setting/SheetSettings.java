package net.officefloor.hq.app.setting;

/** The settings of a sheet being saved: the clerk limit with two decimals, "" for no limit. */
public record SheetSettings(String sheetId, String clerkLimit) {
}
