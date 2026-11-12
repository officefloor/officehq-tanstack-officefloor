package net.officefloor.hq.app.person;

import java.time.LocalDate;
import net.officefloor.hq.app.sheet.InvalidSheetException;

/** The rule a retirement sets: no claim of a retired person dated after the date they are retired from, nor
 *  one dated after the from-date and before the to-date of a past retirement of theirs. */
public final class Retirement {
    private Retirement() {
    }

    public static void refuseClaimDate(Person person, LocalDate date) throws InvalidSheetException {
        if (person.isRetired() && date.isAfter(person.getRetiredFrom())) {
            throw new InvalidSheetException(person.getName() + " is retired from " + person.getRetiredFrom());
        }
        for (PastRetirement past : person.getPastRetirements()) {
            if (past.bars(date)) {
                throw new InvalidSheetException(person.getName() + " was retired from " + past.from() + " to " + past.to());
            }
        }
    }
}
