package net.officefloor.hq.app.sheet;

import net.officefloor.hq.app.setting.Role;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Refuses (400) a clone unless a steward clones the sheet to a sheet never used; publishes the clone. */
public class CheckSheetClone {
    public void check(@Val Sheet sheet, @Val SheetCloneRequest request, @RequestParam("user") String user,
            SettingRepository settings, SheetRepository sheets, Out<SheetClone> clone)
            throws SheetCloneRefusedException {
        if (settings.find(sheet.id()).roleOf(user) != Role.STEWARD) {
            throw new SheetCloneRefusedException("Only a steward can clone the sheet.");
        }
        if (sheets.find(request.to()) != null) {
            throw new SheetCloneRefusedException("The sheet " + request.to() + " is already in use.");
        }
        clone.set(new SheetClone(sheet.id(), request.to()));
    }
}
