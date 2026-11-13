package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.setting.Role;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Refuses (400) a re-date unless a steward re-dates an OPEN or PENDING claim to a date neither after its person's
 * retirement date (it keeps its due-by, so its period: a new date in a closed period is allowed); publishes the re-date.
 */
public class CheckClaimRedate {
    public void check(@RequestParam("user") String user, @Val Claim claim, @Val NewClaimDate newDate,
            SettingRepository settings, PersonRepository people, PeriodRepository periods, Out<Redating> redating)
            throws InvalidClaimException {
        LocalDate date = newDate.value();
        if (settings.find(claim.sheetId()).roleOf(user) != Role.STEWARD) {
            throw new InvalidClaimException("Only a steward can re-date a claim.");
        }
        if (!claim.status().open()) {
            throw new InvalidClaimException("A " + claim.status() + " claim cannot be re-dated.");
        }
        Person person = people.find(claim.sheetId(), claim.personId());
        if (person != null && person.retiredBefore(date)) {
            throw new InvalidClaimException(
                    person.name() + " is retired from " + person.retiredFrom() + ": no claim can be dated " + date + ".");
        }
        redating.set(new Redating(claim.date(), date));
    }
}
