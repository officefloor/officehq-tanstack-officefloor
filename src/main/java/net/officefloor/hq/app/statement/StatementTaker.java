package net.officefloor.hq.app.statement;

import java.time.LocalDate;

/** Who takes a statement, and the date it is taken as of (null: the sheet's today), once the body is read. */
public record StatementTaker(String by, LocalDate asOf) {

    public StatementTaker(String by) {
        this(by, null);
    }
}
