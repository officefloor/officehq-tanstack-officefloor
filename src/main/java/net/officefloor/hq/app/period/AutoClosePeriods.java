package net.officefloor.hq.app.period;

import java.util.Comparator;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.AuditUser;
import net.officefloor.hq.app.claim.ClaimLogRepository;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.person.PersonGradeRepository;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestHeader;

/** Once the sheet's today is changed, with grace days set, closes each open period whose to-date plus the grace
 *  days is before the new today, in order of their to-dates, exactly as a close by hand would (a period the user's
 *  role may not close is left open), recording each closing with auto=yes at its end. */
public class AutoClosePeriods {
    public void close(@Val Sheet sheet, PeriodRepository repository, ClaimRepository claims, PersonRepository people,
            RateRepository rates, PersonGradeRepository grades, ClaimLogRepository log, RoleRepository roles,
            @RequestHeader(name = "X-User", required = false) String user, Audit audit) {
        Integer grace = sheet.getGraceDays();
        if (grace == null) {
            return;
        }
        String by = AuditUser.decode(user);
        if (!sheet.allows("close", roles.roleOf(sheet.getId(), by))) {
            return;
        }
        var due = repository.findBySheetIdOrderByFromDateAscIdAsc(sheet.getId()).stream()
                .filter(p -> !p.isClosed() && p.getToDate().plusDays(grace).isBefore(sheet.getToday()))
                .sorted(Comparator.comparing(Period::getToDate).thenComparing(Period::getId))
                .toList();
        for (Period period : due) {
            ClosePeriod.fix(period, sheet, claims, people, rates, grades, log);
            repository.saveAndFlush(period);
            audit.record("PERIOD_CLOSED sheet=" + sheet.getId() + " by=" + by + " period=" + period.getName()
                    + " total=" + period.getClosedTotal().toPlainString() + " auto=yes");
        }
    }
}
