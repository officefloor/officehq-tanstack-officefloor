package net.officefloor.hq.app.rollup;

import net.officefloor.hq.app.history.AsOf;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.AuditUser;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.person.PersonGradeRepository;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

/** Records, after the change's own lines, each person whose figures the change moved, then the sheet's totals
 *  when they moved. */
public class AuditRollup {
    public void audit(@PathVariable("sheetId") String sheetId, @Val Rollup before, SheetRepository sheets,
            PersonRepository people, ClaimRepository claims, RateRepository rates, PersonGradeRepository grades, AsOf asOf,
            @RequestHeader(name = "X-User", required = false) String user, Audit audit) {
        Rollup now = Rollup.of(sheetId, sheets, people, claims, rates, grades, asOf);
        now.changesSince(before, sheetId, AuditUser.decode(user)).forEach(audit::record);
    }
}
