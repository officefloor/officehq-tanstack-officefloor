package net.officefloor.hq.app.grade;

import net.officefloor.hq.app.setting.Role;
import net.officefloor.hq.app.setting.SettingRepository;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** Refuses (400) a grade merge made by anyone but a steward. */
public class CheckGradeMergeSteward {
    public void check(@PathVariable("sheetId") String sheetId, @RequestParam("user") String user,
            SettingRepository settings) throws InvalidGradeException {
        if (settings.find(sheetId).roleOf(user) != Role.STEWARD) {
            throw new InvalidGradeException("Only a steward may merge a grade.");
        }
    }
}
