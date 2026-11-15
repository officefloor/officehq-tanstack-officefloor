package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.hq.app.period.PeriodClosedException;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.setting.Role;
import net.officefloor.hq.app.setting.RoleRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** A steward re-dates an OPEN or PENDING claim ({@code date}, yyyy-MM-dd). Its due-by stays as it was
 *  fixed and a figure a statement bound it to stays bound, so its period (the one its due-by falls in)
 *  and its number stay too. Refused for anyone else, for any other status, for a claim due by a day
 *  in a closed period, and for a date that is not a date or is after its person's retirement. */
public class RedateClaim {
    public void redate(@RequestParam(name = "by", required = false) String by,
            @RequestParam(name = "date", required = false) String date, @Val Claim claim,
            PersonRepository people, RoleRepository roles, PeriodRepository periods, Out<Redating> redating) throws ClaimStatusException, InvalidClaimException, PeriodClosedException {
        redating.set(redate(by, claim, date, people, roles, periods));
    }

    /** Re-dates the claim in place by the rules above (writing nothing): the date it had and the one it has now. */
    static Redating redate(String by, Claim claim, String date, PersonRepository people, RoleRepository roles,
            PeriodRepository periods) throws ClaimStatusException, InvalidClaimException, PeriodClosedException {
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
        periods.requireOpen(sheetId, claim.getDueBy());
        Person person = people.find(sheetId, claim.getPersonId());
        if (person != null && person.retiredBefore(to)) {
            throw new InvalidClaimException("A claim cannot be dated after its person's retirement");
        }
        String old = claim.getDate();
        claim.setDate(to.toString());
        return new Redating(old, claim.getDate());
    }
}
