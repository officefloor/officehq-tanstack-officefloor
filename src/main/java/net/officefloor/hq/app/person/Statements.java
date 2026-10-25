package net.officefloor.hq.app.person;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A person's statements: how many were taken, and the last one's due-total (null: n/a) and date (null: none taken). */
public record Statements(int count, BigDecimal lastDueTotal, LocalDate lastDate) {
    public static final Statements NONE = new Statements(0, null, null);
}
