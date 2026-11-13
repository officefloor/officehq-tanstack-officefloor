package net.officefloor.hq.app.sheet;

import net.officefloor.hq.app.setting.Role;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Archives the sheet; only a steward (given or delegated) may. */
public class ApplySheetArchived {
    public void apply(@Val Sheet sheet, @RequestParam("user") String user, SettingRepository settings,
            Out<Sheet> updated) throws SheetArchivedException {
        if (settings.find(sheet.id()).roleOf(user) != Role.STEWARD) {
            throw new SheetArchivedException("Only a steward can archive the sheet.");
        }
        updated.set(new Sheet(sheet.id(), sheet.today(), true));
    }
}
