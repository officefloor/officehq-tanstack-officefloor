package net.officefloor.hq.app.sheet;

import net.officefloor.hq.app.history.AsOf;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.AuditUser;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.person.PersonGradeRepository;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestHeader;

/** Records the clone, once it is saved: on this sheet; then on the clone, its first line, followed by its rollup
 *  lines for every person and its totals. */
public class AuditSheetCloned {
    public void audit(@Val SheetCloned cloned, SheetRepository sheets, PersonRepository people, ClaimRepository claims,
            RateRepository rates, PersonGradeRepository grades, AsOf asOf,
            @RequestHeader(name = "X-User", required = false) String userHeader, Audit audit) {
        String user = AuditUser.decode(userHeader);
        String from = cloned.fromSheet();
        String to = cloned.toSheet();
        audit.record("SHEET_CLONE_MADE sheet=" + from + " by=" + user + " to=" + to);
        audit.record("SHEET_CLONED sheet=" + to + " by=" + user + " from=" + from);
        Rollup.of(to, sheets, people, claims, rates, grades, asOf).lines(to, user).forEach(audit::record);
    }
}
