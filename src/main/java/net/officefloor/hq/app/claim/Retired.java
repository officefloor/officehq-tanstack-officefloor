package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import net.officefloor.hq.app.person.Person;

/** No claim may be dated after its person's retirement date. */
final class Retired {

    private Retired() {
    }

    static void refuse(Person person, LocalDate date) throws InvalidClaimException {
        if (!person.claimAllowedOn(date)) {
            throw new InvalidClaimException(person.getName() + " is retired from " + person.getRetiredFrom()
                    + ": no claim may be dated after it.");
        }
    }
}
