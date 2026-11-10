package net.officefloor.hq.app.setting;

import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Val;

/**
 * A parent sheet named must be a sheet that has been loaded, not the sheet itself, and not a sheet
 * that has this sheet as an ancestor. Runs before anything is written.
 */
public class CheckParent {
    public void check(@Val SettingsInput input, @Val Sheet sheet, SheetRepository sheets)
            throws InvalidSettingException {
        if (!input.parentChanged() || input.parent() == null) return;
        String parent = input.parent();
        if (parent.equals(sheet.id())) throw new InvalidSettingException("A sheet cannot be its own parent");
        if (sheets.find(parent) == null) throw new InvalidSettingException("The parent must be a sheet that has been loaded");
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (String at = sheets.parent(parent); at != null && seen.add(at); at = sheets.parent(at)) {
            if (at.equals(sheet.id())) {
                throw new InvalidSettingException("The parent cannot be a sheet that has this sheet as an ancestor");
            }
        }
    }
}
