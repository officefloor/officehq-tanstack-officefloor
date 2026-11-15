package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Records the transfer just made on the sheet the person went to (PERSON_TRANSFERRED ... from=),
 *  then that sheet's ROLLUP/TOTALS lines. */
public class RecordPersonArrived {
    public void record(@RequestParam(name = "by", required = false) String by,
            @Val Transfer transfer, PersonRepository people, ClaimRepository claims, RateRepository rates,
            SheetRepository sheets, SettingRepository settings, Audit audit) {
        String user = by == null ? "" : by;
        String to = transfer.to();
        audit.record("PERSON_TRANSFERRED sheet=" + to + " by=" + user
                + " name=" + transfer.person().getName() + " from=" + transfer.person().getSheetId());
        Rollup after = Rollup.of(to, people, claims, rates, sheets, settings);
        for (String line : after.changesSince(transfer.toBefore(), to, user)) {
            audit.record(line);
        }
    }
}
