package net.officefloor.hq.app.person;

import java.time.LocalDate;
import net.officefloor.hq.app.sheet.InvalidSheetException;

/** The rule a retirement sets: no claim of a retired person dated after the date they are retired from. */
public final class Retirement {
    private Retirement() {
    }

    public static void refuseClaimDate(Person person, LocalDate date) throws InvalidSheetException {
        if (person.isRetired() && date.isAfter(person.getRetiredFrom())) {
            throw new InvalidSheetException(person.getName() + " is retired from " + person.getRetiredFrom());
        }
    }
}
