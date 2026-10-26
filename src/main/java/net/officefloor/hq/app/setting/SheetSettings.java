package net.officefloor.hq.app.setting;

/**
 * The settings of a sheet being saved, each null when it is left as it is: the clerk limit with two decimals,
 * "" for no limit, and the floor with two decimals.
 */
public record SheetSettings(String sheetId, String clerkLimit, String floor) {
}
