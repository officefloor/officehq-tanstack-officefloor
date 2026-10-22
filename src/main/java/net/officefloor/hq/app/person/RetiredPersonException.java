package net.officefloor.hq.app.person;

import java.time.LocalDate;

/** A claim dated after its person's retirement date. */
public class RetiredPersonException extends Exception {
    public RetiredPersonException(Person person, LocalDate date) {
        super(person.name() + " is retired from " + person.retiredFrom() + ": a claim dated " + date + " is after that.");
    }

    /** Refuse a claim dated after the person's retirement date. */
    public static void refuse(Person person, LocalDate date) throws RetiredPersonException {
        if (person.retiredFrom() != null && date.isAfter(person.retiredFrom())) {
            throw new RetiredPersonException(person, date);
        }
    }
}
