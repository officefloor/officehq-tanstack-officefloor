package net.officefloor.hq.app.setting;

import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;

public class SaveNoteThreshold {
    public void save(@Val NoteThreshold threshold, @Val Sheet sheet, SettingRepository repository) {
        if (threshold.given()) {
            repository.saveDated(sheet.id(), DatedSettings.NOTE_THRESHOLD,
                    DatedSettings.fromOr(threshold.from(), sheet.today()),
                    threshold.value() == null ? null : threshold.value().toPlainString());
        }
    }
}
