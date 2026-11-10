package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import java.util.Objects;
import jakarta.servlet.http.HttpServletRequest;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.period.ClosedPeriodException;
import net.officefloor.hq.app.period.Period;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.person.RetiredPersonException;
import net.officefloor.hq.app.role.Role;
import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.hq.app.role.StewardOnlyException;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.setting.SettingsRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/**
 * Re-dates an OPEN or PENDING claim (a steward). Its new date may not be after its person's
 * retirement nor inside a closed period. Its due-by and any figure a statement bound it to are kept;
 * its period follows the new date, and while numbering by period, a claim whose period changes takes
 * the next number of its new period's sequence (its old number is never used again).
 */
public class RedateClaim {
    public void apply(@Val Sheet sheet, @Val Claim claim, @Val Redate redate, PersonRepository people,
            RoleRepository roles, SettingsRepository settings, PeriodRepository periods, ClaimRepository claims,
            HttpServletRequest request, Out<Redating> redatingOut)
            throws StewardOnlyException, InvalidClaimStatusException, RetiredPersonException, ClosedPeriodException {
        if (!Role.STEWARD.equals(roles.roleOf(sheet.id(), Audit.user(request)))) {
            throw new StewardOnlyException("Only a steward may re-date a claim");
        }
        if (!claim.isOpen()) {
            throw new InvalidClaimStatusException("A " + claim.getStatus() + " claim cannot be re-dated");
        }
        LocalDate to = redate.date();
        Person person = people.find(sheet.id(), claim.getPersonId());
        if (person != null && person.retiredBefore(to)) throw new RetiredPersonException(person.getName());
        Period closed = periods.closedOn(sheet.id(), to);
        if (closed != null) throw new ClosedPeriodException(closed.name());

        LocalDate from = claim.getDate();
        String periodBefore = periodOf(periods, sheet, from);
        claim.setDate(to);
        Settings sheetSettings = settings.find(sheet.id());
        if (sheetSettings.numbersByPeriod(sheet.today()) && !Objects.equals(periodBefore, periodOf(periods, sheet, to))) {
            claim.setNumber(ClaimNumbers.next(sheet, claim, sheetSettings, periods, claims));
        }
        redatingOut.set(new Redating(from, to));
    }

    private static String periodOf(PeriodRepository periods, Sheet sheet, LocalDate date) {
        return periods.findAll(sheet.id()).stream().filter((p) -> p.contains(date))
                .map(Period::name).findFirst().orElse(null);
    }
}
