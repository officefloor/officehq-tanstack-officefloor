package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.time.LocalDate;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.period.Periods;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.rate.Rates;
import net.officefloor.hq.app.role.Role;
import net.officefloor.hq.app.role.Roles;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.sheet.SheetClock;
import net.officefloor.hq.app.watch.Watches;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * A steward re-settles a SETTLED claim outside a closed period: its due is fixed again at the figure it would have
 * today as an OPEN claim bound by no statement (today's rates, grades, floor and late charge), and its status log
 * gains a SETTLED line.
 */
public class ResettleClaim {
    public void resettle(@PathVariable("sheet") String sheet, @RequestParam("user") String user, @Val Claim claim,
            Claims claims, People people, Rates rates, Periods periods, Roles roles, Settings settings,
            SheetClock clock, Audit audit, Rollup rollup, ClaimLog log, Watches watches) throws InvalidClaimStatusChangeException {
        if (!Role.STEWARD.equals(roles.of(sheet, user))) {
            throw new InvalidClaimStatusChangeException("Only a steward re-settles a claim");
        }
        if (claim.status() != ClaimStatus.SETTLED) {
            throw new InvalidClaimStatusChangeException("A " + claim.status() + " claim cannot be re-settled");
        }
        if (periods.closedOn(sheet, LocalDate.parse(claim.date()))) {
            throw new InvalidClaimStatusChangeException("A claim dated inside a closed period cannot be re-settled");
        }
        Person person = people.find(sheet, claim.person());
        BigDecimal was = claim.settledDue() == null ? null : new BigDecimal(claim.settledDue());
        BigDecimal due = claim.asOpenUnbound().due(rates.list(sheet), person.gradeOn(claim.date()),
                settings.floor(sheet), settings.late(sheet, clock.today(sheet)));
        Rollup.Snapshot before = rollup.take(sheet);
        if (!claims.resettle(sheet, claim.id(), was, due)) {
            throw new InvalidClaimStatusChangeException("The claim changed since it was read");
        }
        log.record(sheet, user, claim.id());
        audit.record("CLAIM_RESETTLED sheet=" + sheet + " by=" + user + " " + claim.statusAuditFields(person.name())
                + " old=" + (was == null ? "n/a" : was.toPlainString())
                + " new=" + (due == null ? "n/a" : due.toPlainString()));
        watches.notify(sheet, user, person.id(), person.name(), claim.shownNumber());
        rollup.record(sheet, user, before);
    }
}
