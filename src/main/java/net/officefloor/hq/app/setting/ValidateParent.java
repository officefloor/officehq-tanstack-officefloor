package net.officefloor.hq.app.setting;

import java.util.HashSet;
import java.util.Set;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Refuse a parent sheet (when the save sets one) that is the sheet itself, or a sheet that has never been loaded,
 *  or a sheet that has this sheet as an ancestor (its parent, its parent's parent, and so on). */
public class ValidateParent {
    public void validate(@PathVariable("sheetId") String sheetId, @Val SettingsInput input, SheetRepository sheets,
            SettingRepository settings) throws InvalidSettingException {
        if (!input.setsParent() || input.parent() == null) {
            return;
        }
        String id = sheetId.trim();
        String parent = input.parent();
        if (parent.equals(id)) {
            throw new InvalidSettingException("A sheet cannot be its own parent.");
        }
        if (!sheets.exists(parent)) {
            throw new InvalidSettingException("The parent must be a sheet that has been loaded.");
        }
        Set<String> seen = new HashSet<>();
        for (String at = settings.parent(parent); at != null && seen.add(at); at = settings.parent(at)) {
            if (at.equals(id)) {
                throw new InvalidSettingException("The parent cannot be a sheet that has this sheet as an ancestor.");
            }
        }
    }
}
