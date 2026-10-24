package net.officefloor.hq.app.setting;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.AuditUser;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestHeader;

/** Records each setting the form set, once it is saved, with the value saved: the clerk limit (blank when there is none), then the floor, then the terms, then the late percent, then the policy date (blank when it is the sheet's today), then the period numbering from (blank when off), then each grade's own terms (key terms-<grade>; blank when it uses the sheet's terms), then the cap (blank when there is none), then the proof threshold (blank when there is none), then the permission matrix entries, then the grace days, then the note threshold (blank when there is none), then the entry date (blank when off), then each grade's grace days (key grace-<grade>; blank when none), then each grade's proof threshold (key proof-threshold-<grade>; blank when none), then the pot (blank when there is none); a dated setting ends with from=<date> when a from-date was given. */
public class AuditSettingChanged {
    public void audit(@Val SettingsInput input, @Val Sheet sheet,
            @RequestHeader(name = "X-User", required = false) String user, Audit audit) {
        String head = "SETTING_CHANGED sheet=" + sheet.getId() + " by=" + AuditUser.decode(user);
        for (SettingsInput.Change change : input.changes()) {
            audit.record(head + " key=" + change.key() + " value=" + (change.value() == null ? "" : change.value())
                    + (change.from() == null ? "" : " from=" + change.from()));
        }
    }
}
