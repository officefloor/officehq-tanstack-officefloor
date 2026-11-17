package net.officefloor.hq.app.sheet;

import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Only a role the sheet's today permission allows (anyone but a viewer while it is blank) may change its today. */
public class PermitToday {
    public void permit(@PathVariable("sheetId") String sheetId, @Val TodayBy by, RoleRepository roles,
            SettingRepository settings) throws InvalidTodayException {
        String sheet = sheetId.trim();
        if (!settings.permits(sheet, "today", roles.roleOf(sheet, by.by()))) {
            throw new InvalidTodayException("The sheet's permission does not let this user change its today");
        }
    }
}
