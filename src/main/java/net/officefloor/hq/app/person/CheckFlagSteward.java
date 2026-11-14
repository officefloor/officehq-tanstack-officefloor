package net.officefloor.hq.app.person;

import net.officefloor.hq.app.setting.Role;
import net.officefloor.hq.app.setting.SettingRepository;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** Refuses (400) a flag made by anyone but a steward. */
public class CheckFlagSteward {
    public void check(@PathVariable("sheetId") String sheetId, @RequestParam("user") String user,
            SettingRepository settings) throws InvalidPersonException {
        if (settings.find(sheetId).roleOf(user) != Role.STEWARD) {
            throw new InvalidPersonException("Only a steward may flag a person.");
        }
    }
}
