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

/** Records, after the transfer back's own lines, that the transfer was undone on this sheet, then its rollup lines. */
public class AuditTransferUndone {
    public void audit(@Val PersonTransferred transferred, @Val Rollup before, SheetRepository sheets,
            PersonRepository people, ClaimRepository claims, RateRepository rates, PersonGradeRepository grades, AsOf asOf,
            @RequestHeader(name = "X-User", required = false) String userHeader, Audit audit) {
        String user = AuditUser.decode(userHeader);
        String here = transferred.fromSheet();
        audit.record("PERSON_TRANSFER_UNDONE sheet=" + here + " by=" + user + " name=" + transferred.name() + " to="
                + transferred.toSheet());
        Rollup.of(here, sheets, people, claims, rates, grades, asOf).changesSince(before, here, user)
                .forEach(audit::record);
    }
}
