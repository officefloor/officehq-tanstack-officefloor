package net.officefloor.hq.app.person;

import java.time.LocalDate;

/** A claim dated after its person's retirement date, or within one of their past retirements. */
public class RetiredPersonException extends Exception {
    public RetiredPersonException(Person person, LocalDate date) {
        super(person.name() + " is retired from " + person.retiredFrom() + ": a claim dated " + date + " is after that.");
    }

    public RetiredPersonException(Person person, Person.Retirement past, LocalDate date) {
        super(person.name() + " was retired from " + past.from() + " to " + past.to() + ": a claim dated " + date
                + " is within that.");
    }

    /** Refuse a claim dated after the person's retirement date, or after the from and before the to of one of their
     *  past retirements. */
    public static void refuse(Person person, LocalDate date) throws RetiredPersonException {
        if (person.retiredFrom() != null && date.isAfter(person.retiredFrom())) {
            throw new RetiredPersonException(person, date);
        }
        for (Person.Retirement past : person.retirements()) {
            if (past.covers(date)) {
                throw new RetiredPersonException(person, past, date);
            }
        }
    }
}
