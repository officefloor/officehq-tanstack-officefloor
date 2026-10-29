package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

public class ApplyEscalation {
    // With an escalation gap set, a reminder whose previous one (on the claim as loaded) was at least that many days
    // before the sheet's today escalates the claim, once per claim ever.
    public void apply(@Val Claim loaded, Settings settings, Sheets sheets, Claims claims, Out<ClaimEscalation> escalationOut) {
        Integer gap = settings.escalateAfter(loaded.sheetId());
        LocalDate previous = loaded.remindedOn();
        LocalDate today = sheets.today(loaded.sheetId());
        boolean escalated = gap != null && previous != null && !previous.plusDays(gap).isAfter(today) && claims.escalate(loaded);
        escalationOut.set(new ClaimEscalation(escalated));
    }
}
