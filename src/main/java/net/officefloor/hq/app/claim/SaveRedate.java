package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import net.officefloor.hq.app.period.Period;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.person.RetiredPersonException;
import net.officefloor.hq.app.role.Role;
import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Re-date the loaded claim: only a steward may, and only an OPEN or PENDING claim, to a date its person may claim
 *  for (not after their retirement). Its due-by and any figure a statement bound it to stay as they are. Its period
 *  follows the new date: while the sheet numbers claims by period, a claim whose period changes takes the next
 *  number of its new period's sequence (X-<n> for no period), and its old number is never given out again. A
 *  re-date another user's change got to first is refused. */
public class SaveRedate {
    public void save(@Val RedateInput input, @Val Claim loaded, ClaimRepository repository, PersonRepository people,
            PeriodRepository periods, RoleRepository roles, Out<ClaimResponse> saved)
            throws ClaimStatusNotAllowedException, RetiredPersonException {
        if (!Role.STEWARD.equals(roles.of(loaded.sheetId(), input.user()))) {
            throw new ClaimStatusNotAllowedException("Only a steward may re-date a claim.");
        }
        if (!loaded.status().unsettled()) {
            throw new ClaimStatusNotAllowedException("A " + loaded.status()
                    + " claim cannot be re-dated; only an OPEN or PENDING one can.");
        }
        RetiredPersonException.refuse(people.find(loaded.sheetId(), loaded.personId()), input.date());
        String number = loaded.number();
        if (repository.numberingByPeriod(loaded.sheetId())) {
            var all = periods.list(loaded.sheetId());
            if (!Objects.equals(periodOf(all, loaded.date()), periodOf(all, input.date()))) {
                number = repository.nextNumber(loaded.sheetId(), input.date());
            }
        }
        if (!repository.redate(loaded, input.date(), number)) {
            throw new ClaimStatusNotAllowedException("The claim changed meanwhile; load the sheet again.");
        }
        saved.set(ClaimResponse.from(new Claim(loaded.id(), loaded.sheetId(), loaded.personId(), loaded.amount(),
                input.date(), loaded.note(), loaded.status(), number, loaded.settledDue(), loaded.openedBy(),
                loaded.stated(), loaded.statedDue(), loaded.corrects(), loaded.dueBy(), loaded.deferredUntil(),
                loaded.proofs(), loaded.splitFrom(), loaded.lateEntry())));
    }

    /** The name of the period the date falls in (the earliest that holds it, as numbering takes it), else null. */
    private static String periodOf(List<Period> all, LocalDate date) {
        return all.stream().filter(p -> p.contains(date)).map(Period::name).findFirst().orElse(null);
    }
}
