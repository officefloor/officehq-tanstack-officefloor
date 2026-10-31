package net.officefloor.hq.app.person;

import java.time.LocalDate;

/** A claim dated after its person's retirement date, or within a past retirement; answered 409. */
public class PersonRetiredException extends Exception {
    public PersonRetiredException(String name, LocalDate from) {
        super(name + " is retired from " + from + ": no claim dated after it can be added");
    }

    /** A claim dated within one of its person's past retirements. */
    public PersonRetiredException(String name, PastRetirement past) {
        super(name + " was retired from " + past.from() + " to " + past.to()
                + ": no claim dated between them can be added");
    }
}
