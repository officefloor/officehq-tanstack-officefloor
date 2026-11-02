package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import net.officefloor.hq.app.holiday.Holidays;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.period.Periods;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.sheet.SheetClock;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

public class BuildClaim {
    public void build(@PathVariable("sheet") String sheet, @RequestParam("user") String user, @Val Person person,
            @Val ClaimInput input, Claims claims, Periods periods, Settings settings, Holidays holidays, SheetClock clock, ClaimLog log, Audit audit, Rollup rollup)
            throws InvalidClaimException {
        // The due-by is fixed now, from the terms in force on the sheet's today (the day the claim is made); the
        // claim belongs to the period its due-by falls in.
        LocalDate today = clock.today(sheet);
        LocalDate dueBy = person.dueBy(sheet, input.date(), today, settings, holidays);
        if (periods.closedOn(sheet, dueBy)) {
            throw new InvalidClaimException("A claim may not be due inside a closed period");
        }
        if (person.retiredBefore(input.date())) {
            throw new InvalidClaimException("A claim may not be dated after the person's retirement date");
        }
        input.checkCorrects(sheet, person.id(), null, claims);
        input.checkRef(sheet, null, claims);
        Rollup.Snapshot before = rollup.take(sheet);
        long id = claims.add(sheet, person.id(), input, dueBy, person.termsOn(sheet, today, settings));
        log.record(sheet, user, id);
        audit.record("CLAIM_ADDED sheet=" + sheet + " by=" + user + " " + input.addedAuditFields(person.name()));
        if (periods.softClosedOn(sheet, dueBy)) {
            claims.setLateEntry(sheet, id);
            audit.record("LATE_ENTRY sheet=" + sheet + " by=" + user + " " + input.auditFields(person.name()));
        }
        rollup.record(sheet, user, before);
    }
}
