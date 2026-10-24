package net.officefloor.hq.app.sheet;

import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Refuse a change of the sheet's today by a role the sheet's permission matrix does not allow it (by default anyone
 *  but a viewer). */
public class RefuseTodayChange {
    public void refuse(@PathVariable("sheetId") String sheetId, @Val TodayRequest body, RoleRepository roles,
            SettingRepository settings) throws InvalidSheetException {
        String id = sheetId.trim();
        if (!settings.permissions(id).allows("today", roles.of(id, body.user()))) {
            throw new InvalidSheetException("Your role may not change the sheet's today.");
        }
    }
}
