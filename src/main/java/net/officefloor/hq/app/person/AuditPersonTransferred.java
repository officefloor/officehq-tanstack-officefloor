package net.officefloor.hq.app.person;

import net.officefloor.hq.app.history.AsOf;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.AuditUser;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestHeader;

/** Records the transfer, once it is saved: on the sheet left, then its rollup lines; on the sheet arrived on (then the
 *  merge, when they were merged into someone there), then its rollup lines. */
public class AuditPersonTransferred {
    public void audit(@Val PersonTransferred transferred, @Val Rollup before, SheetRepository sheets,
            PersonRepository people, ClaimRepository claims, RateRepository rates, PersonGradeRepository grades, AsOf asOf,
            @RequestHeader(name = "X-User", required = false) String userHeader, Audit audit) {
        String user = AuditUser.decode(userHeader);
        String from = transferred.fromSheet();
        String to = transferred.toSheet();
        audit.record("PERSON_TRANSFERRED sheet=" + from + " by=" + user + " name=" + transferred.name() + " to=" + to);
        Rollup.of(from, sheets, people, claims, rates, grades, asOf).changesSince(before, from, user)
                .forEach(audit::record);
        audit.record("PERSON_TRANSFERRED sheet=" + to + " by=" + user + " name=" + transferred.name() + " from=" + from);
        if (transferred.mergedInto() != null) {
            audit.record("PERSON_MERGED sheet=" + to + " by=" + user + " from=" + transferred.name() + " into="
                    + transferred.mergedInto());
        }
        Rollup.of(to, sheets, people, claims, rates, grades, asOf).changesSince(transferred.toBefore(), to, user)
                .forEach(audit::record);
    }
}
