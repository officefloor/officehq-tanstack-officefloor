package net.officefloor.hq.app.setting;

import net.officefloor.hq.app.sheet.Sheet;

/** The sheet's settings as the screen shows them: the clerk limit with two decimals (null: no limit) and the
 *  floor with two decimals. */
public record SettingsResponse(String clerkLimit, String floor) {

    public static SettingsResponse from(Sheet sheet) {
        return new SettingsResponse(sheet.getClerkLimit() == null ? null : sheet.getClerkLimit().setScale(2).toPlainString(),
                sheet.getFloor().setScale(2).toPlainString());
    }
}
