package net.officefloor.hq.app.person;

import java.time.LocalDate;

/** A claim dated after its person's retirement date; answered 409. */
public class PersonRetiredException extends Exception {
    public PersonRetiredException(String name, LocalDate from) {
        super(name + " is retired from " + from + ": no claim dated after it can be added");
    }
}
