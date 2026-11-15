package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Objects;
import net.officefloor.hq.app.period.Period;
import net.officefloor.hq.app.period.PeriodClosedException;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.setting.Role;
import net.officefloor.hq.app.setting.RoleRepository;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** A steward re-dates an OPEN or PENDING claim ({@code date}, yyyy-MM-dd). Its due-by stays as it was
 *  fixed and a figure a statement bound it to stays bound; its period follows the new date, and while
 *  period numbering is in force a claim whose period changes takes the next number of its new
 *  period's sequence (its old number is never used again). Refused for anyone else, for any other
 *  status, for a claim dated in a closed period or a new date inside one, and for a date that is not
 *  a date or is after its person's retirement. */
public class RedateClaim {
    public void redate(@RequestParam(name = "by", required = false) String by,
            @RequestParam(name = "date", required = false) String date, @Val Claim claim,
            PersonRepository people, RoleRepository roles, PeriodRepository periods,
            SettingRepository settings, SheetRepository sheets, ClaimRepository claims,
            Out<Redating> redating) throws ClaimStatusException, InvalidClaimException, PeriodClosedException {
        String sheetId = claim.getSheetId();
        if (!Role.STEWARD.equals(roles.roleOf(sheetId, by == null ? "" : by))) {
            throw new ClaimStatusException("Only a steward can re-date a claim");
        }
        if (!ClaimStatus.isOpen(claim.getStatus())) {
            throw new ClaimStatusException("A " + claim.getStatus() + " claim cannot be re-dated");
        }
        LocalDate to;
        try {
            to = LocalDate.parse(date == null ? "" : date.trim());
        } catch (DateTimeParseException e) {
            throw new InvalidClaimException("Not a date: " + date);
        }
        periods.requireOpen(sheetId, claim.getDate());
        periods.requireOpen(sheetId, to.toString());
        Person person = people.find(sheetId, claim.getPersonId());
        if (person != null && person.retiredBefore(to)) {
            throw new InvalidClaimException("A claim cannot be dated after its person's retirement");
        }
        String old = claim.getDate();
        String oldPeriod = periodOf(periods, sheetId, old);
        claim.setDate(to.toString());
        LocalDate numberingFrom = settings.periodNumberingFrom(sheetId);
        boolean numbering = numberingFrom != null && !sheets.today(sheetId).isBefore(numberingFrom);
        if (numbering && !Objects.equals(oldPeriod, periodOf(periods, sheetId, claim.getDate()))) {
            claim.setNumber(ClaimNumbers.next(claim, claims, periods, settings, sheets));
        }
        redating.set(new Redating(old, claim.getDate()));
    }

    private static String periodOf(PeriodRepository periods, String sheetId, String date) {
        return periods.list(sheetId).stream().filter((p) -> p.contains(date)).map(Period::name)
                .findFirst().orElse(null);
    }
}
