package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/** A validated claim body: a blank date already defaulted to the sheet's today. */
public record ClaimInput(BigDecimal amount, LocalDate date, String note) {

    /** The audit fields of this claim for the named person: amount as the screen shows it, ISO date. */
    public String auditFields(String person) {
        return "person=" + person + " amount=" + amount.setScale(2, RoundingMode.UNNECESSARY).toPlainString()
                + " date=" + date;
    }
}
