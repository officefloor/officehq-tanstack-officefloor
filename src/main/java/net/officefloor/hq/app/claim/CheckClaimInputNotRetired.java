package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Refuses (400) a claim added to a date (blank: the sheet's today) after its person's retirement date. */
public class CheckClaimInputNotRetired {
    public void check(@Val ClaimInput input, @Val Person person, @PathVariable("sheetId") String sheetId,
            SheetRepository sheets) throws InvalidClaimException {
        LocalDate date = input.date() != null ? input.date() : sheets.today(sheetId);
        if (person.retiredBefore(date)) {
            throw new InvalidClaimException(
                    person.name() + " is retired from " + person.retiredFrom() + ": no claim can be dated " + date + ".");
        }
    }
}
