package net.officefloor.hq.app.setting;

import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Val;

/** Sets the sheet's clerk limit and floor, those the form sets. */
public class ApplySettings {
    public void apply(@Val SettingsInput input, @Val Sheet sheet, SheetRepository repository) {
        if (input.setClerkLimit()) {
            sheet.setClerkLimit(input.clerkLimit());
        }
        if (input.setFloor()) {
            sheet.setFloor(input.floor());
        }
        repository.saveAndFlush(sheet);
    }
}
