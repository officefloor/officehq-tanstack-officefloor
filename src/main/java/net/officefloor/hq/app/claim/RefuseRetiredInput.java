package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.Retirement;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.plugin.variable.Val;

public class RefuseRetiredInput {
    // The date a claim is being added to (a blank date is the entry date, else the sheet's today) must not be after its person's retirement,
    // nor strictly between the dates of one of their past retirements.
    public void refuse(@Val ClaimInput input, @Val Person person, Settings settings) throws ClaimAfterRetirementException {
        check(person, input.date() != null ? input.date() : settings.entryDate(person.sheetId()));
    }

    static void check(Person person, LocalDate date) throws ClaimAfterRetirementException {
        if (person != null && person.retired() && date.isAfter(person.retiredFrom())) {
            throw new ClaimAfterRetirementException(person.name() + " is retired from " + person.retiredFrom()
                    + ": no claim dated " + date + " can be added or edited.");
        }
        if (person != null) {
            for (Retirement past : person.retirements()) {
                if (past.excludes(date)) {
                    throw new ClaimAfterRetirementException(person.name() + " was retired from " + past.from() + " to "
                            + past.to() + ": no claim dated " + date + " can be added or edited.");
                }
            }
        }
    }
}
