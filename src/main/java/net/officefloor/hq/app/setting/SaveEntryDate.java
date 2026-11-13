package net.officefloor.hq.app.setting;

import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;

public class SaveEntryDate {
    public void save(@Val EntryDate entryDate, @Val Sheet sheet, SettingRepository repository) {
        if (entryDate.given()) {
            repository.saveEntryDate(sheet.id(), entryDate.value());
        }
    }
}
