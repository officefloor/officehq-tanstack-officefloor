package net.officefloor.hq.app.person;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A statement taken for a person: their due-total then (null when n/a) and how many of their
 *  claims counted, by whom, and the date it was taken as of (null: taken now). */
public record Statement(BigDecimal dueTotal, int claims, String user, LocalDate asOf) {

    public Statement(BigDecimal dueTotal, int claims, String user) {
        this(dueTotal, claims, user, null);
    }
}
