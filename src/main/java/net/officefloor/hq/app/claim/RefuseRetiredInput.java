package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.plugin.variable.Val;

public class RefuseRetiredInput {
    // The date a claim is being added to (a blank date is the sheet's today) must not be after its person's retirement.
    public void refuse(@Val ClaimInput input, @Val Person person, Sheets sheets) throws ClaimAfterRetirementException {
        check(person, input.date() != null ? input.date() : sheets.today(person.sheetId()));
    }

    static void check(Person person, LocalDate date) throws ClaimAfterRetirementException {
        if (person != null && person.retired() && date.isAfter(person.retiredFrom())) {
            throw new ClaimAfterRetirementException(person.name() + " is retired from " + person.retiredFrom()
                    + ": no claim dated " + date + " can be added or edited.");
        }
    }
}
