package net.officefloor.hq.app.setting;

import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Val;

/** Sets the sheet's clerk limit. */
public class ApplySettings {
    public void apply(@Val SettingsInput input, @Val Sheet sheet, SheetRepository repository) {
        sheet.setClerkLimit(input.clerkLimit());
        repository.saveAndFlush(sheet);
    }
}
