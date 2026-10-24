package net.officefloor.hq.app.setting;

import java.util.HashSet;
import java.util.Set;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Val;

/** Refuses a parent sheet the form sets that is the sheet itself, or a sheet that has never been loaded, or one that
 *  has the sheet as an ancestor (its parent, its parent's parent, and so on). A blank parent (none) is not checked. */
public class ValidateParent {
    public void validate(@Val SettingsInput input, @Val Sheet sheet, SheetRepository sheets) throws InvalidSettingsException {
        for (SettingsInput.Change change : input.changes()) {
            if (!Sheet.PARENT.equals(change.key()) || change.value() == null) {
                continue;
            }
            String parent = change.value();
            if (parent.equals(sheet.getId())) {
                throw new InvalidSettingsException("A sheet cannot be its own parent.");
            }
            if (!sheets.existsById(parent)) {
                throw new InvalidSettingsException("The parent sheet " + parent + " has never been loaded.");
            }
            Set<String> seen = new HashSet<>();
            for (String id = parent; id != null && seen.add(id); ) {
                Sheet ancestor = sheets.findById(id).orElse(null);
                id = ancestor == null ? null : ancestor.getParent();
                if (sheet.getId().equals(id)) {
                    throw new InvalidSettingsException("The parent sheet " + parent + " has this sheet as an ancestor.");
                }
            }
        }
    }
}
