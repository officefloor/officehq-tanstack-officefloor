package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A validated claim form: the amount, the date ({@code null} when left blank: the sheet's today) and the note. */
public record ClaimInput(BigDecimal amount, LocalDate date, String note) {
}
