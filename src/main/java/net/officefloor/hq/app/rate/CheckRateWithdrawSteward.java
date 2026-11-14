package net.officefloor.hq.app.rate;

import net.officefloor.hq.app.setting.Role;
import net.officefloor.hq.app.setting.SettingRepository;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** Refuses (400) a rate withdrawal made by anyone but a steward. */
public class CheckRateWithdrawSteward {
    public void check(@PathVariable("sheetId") String sheetId, @RequestParam("user") String user,
            SettingRepository settings) throws InvalidRateException {
        if (settings.find(sheetId).roleOf(user) != Role.STEWARD) {
            throw new InvalidRateException("Only a steward may withdraw a rate.");
        }
    }
}
