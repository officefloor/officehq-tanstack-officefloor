package net.officefloor.hq.app.person;

import jakarta.servlet.http.HttpServletRequest;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.setting.SettingsRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Val;

/**
 * Records the transfer on the sheet the person went to, followed by that sheet's rollup lines: the
 * figures their arrival moved there.
 */
public class AuditPersonArrived {
    public void audit(@Val Sheet sheet, @Val Transfer transfer, SheetRepository sheets, PersonRepository people,
            ClaimRepository claims, RateRepository rates, SettingsRepository settings, HttpServletRequest request,
            Audit audit) {
        String to = transfer.to().id();
        String user = Audit.user(request);
        audit.record("PERSON_TRANSFERRED sheet=" + to + " by=" + user
                + " name=" + transfer.person().getName() + " from=" + sheet.id());
        Rollup after = Rollup.of(to, sheets.today(to), people, claims, rates, settings.find(to));
        after.changesSince(transfer.toBefore(), to, user).forEach(audit::record);
    }
}
