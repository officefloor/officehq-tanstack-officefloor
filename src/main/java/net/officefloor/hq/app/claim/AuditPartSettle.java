package net.officefloor.hq.app.claim;

import java.util.Objects;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.AuditUser;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonGradeRepository;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.hq.app.watch.Watch;
import net.officefloor.hq.app.watch.WatchRepository;
import net.officefloor.plugin.section.clazz.Parameter;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestHeader;

/** Records a part-settle, once it is saved, as the split and then the settle would each record themselves: the
 *  split's lines and the figures it moved, then the new claim's settle (with its due), its watchers' notices and the
 *  figures the settle moved. */
public class AuditPartSettle {
    public void audit(@Parameter PartSettle part, @Val Claim claim, @Val Rollup before, SheetRepository sheets,
            PersonRepository people, ClaimRepository claims, RateRepository rates, PersonGradeRepository grades,
            WatchRepository watches, @RequestHeader(name = "X-User", required = false) String user, Audit audit) {
        String person = people.findById(claim.getPersonId()).map(Person::getName).orElse("");
        String by = AuditUser.decode(user);
        Claim made = part.split().made();
        audit.record(ClaimAudit.line("CLAIM_SPLIT", claim, person, by) + " into=" + Objects.requireNonNullElse(made.getNumber(), ""));
        audit.record(ClaimAudit.line(ClaimStatus.OPEN.event(), made, person, by, null));
        part.between().changesSince(before, claim.getSheetId(), by).forEach(audit::record);

        String line = ClaimAudit.line(made.getStatus().event(), made, person, by, part.number());
        if (made.getStatus() == ClaimStatus.SETTLED) {
            line += " due=" + (made.getDue() == null ? "n/a" : made.getDue().setScale(2).toPlainString());
            audit.record(line);
            for (Watch watch : watches.findByPersonIdOrderByIdAsc(made.getPersonId())) {
                audit.record("NOTIFY sheet=" + made.getSheetId() + " by=" + by + " user=" + watch.getUserName()
                        + " person=" + person + " number=" + made.getNumber());
            }
        } else {
            audit.record(line);
        }
        Rollup now = Rollup.of(claim.getSheetId(), sheets, people, claims, rates, grades);
        now.changesSince(part.between(), claim.getSheetId(), by).forEach(audit::record);
    }
}
