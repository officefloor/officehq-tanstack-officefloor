package net.officefloor.hq.app.period;

import java.time.LocalDate;

/** A claim change refused because the claim is due by in a closed period; answered 409. */
public class PeriodClosedException extends Exception {
    public PeriodClosedException(Period period, LocalDate date) {
        super("Period " + period.name() + " is closed: no claim due by " + date + " can be changed");
    }

    public PeriodClosedException(String message) {
        super(message);
    }
}
