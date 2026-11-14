package net.officefloor.hq.app.sheet;

import net.officefloor.hq.app.setting.Permissions;
import net.officefloor.hq.app.setting.SettingRepository;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Refuses (400) a change of the sheet's today when the permission matrix does not allow the user's role it (standing
 * rule: anyone but a viewer). It runs before anything is written.
 */
public class CheckTodayPermission {
    public void check(@PathVariable("sheetId") String sheetId, @RequestParam("user") String user,
            SettingRepository settings) throws TodayRefusedException {
        if (!settings.find(sheetId).may(user, Permissions.TODAY)) {
            throw new TodayRefusedException("The user's role is not allowed to change the sheet's today.");
        }
    }
}
