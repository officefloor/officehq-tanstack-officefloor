package net.officefloor.hq.app.setting;

import java.util.HashSet;
import java.util.Set;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Val;

/**
 * Refuses (400) a parent that is the sheet itself, that was never loaded, or that has the sheet as an ancestor; it runs
 * before anything is saved, so a refused parent writes nothing.
 */
public class CheckParent {
    public void check(@Val Parent parent, @Val Sheet sheet, SheetRepository sheets, SettingRepository settings)
            throws InvalidSettingException {
        if (!parent.given() || parent.value() == null) {
            return;
        }
        if (parent.value().equals(sheet.id())) {
            throw new InvalidSettingException("A sheet cannot be its own parent.");
        }
        if (sheets.find(parent.value()) == null) {
            throw new InvalidSettingException("The parent must be a sheet that has been loaded.");
        }
        Set<String> seen = new HashSet<>();
        for (String up = settings.parent(parent.value()); up != null && seen.add(up); up = settings.parent(up)) {
            if (up.equals(sheet.id())) {
                throw new InvalidSettingException("The parent cannot be a sheet that has this sheet as an ancestor.");
            }
        }
    }
}
