package net.officefloor.hq.app.sheet;

import net.officefloor.hq.app.setting.Permissions;
import net.officefloor.hq.app.setting.RoleRepository;
import net.officefloor.hq.app.setting.SettingRepository;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** Refuses changing the sheet's today for a role the permission matrix does not let change it (by
 *  default anyone but a viewer), before anything is read or written. */
public class PermitSheetToday {
    public void permit(@PathVariable("sheetId") String sheetId,
            @RequestParam(name = "by", required = false) String by, RoleRepository roles,
            SettingRepository settings) throws InvalidSheetTodayException {
        if (!Permissions.allows(sheetId.trim(), by, Permissions.TODAY, roles, settings)) {
            throw new InvalidSheetTodayException("Your role may not change the sheet's today");
        }
    }
}
