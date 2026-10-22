package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A person's claim: an amount (0.00 or more, two decimals), a date, a note (may be empty), its
 *  status, its number on the sheet (null while a draft, or once voided) and, once settled, the due fixed
 *  at settling (null before then, or when the person's grade had no rate then: its due stays live). */
public record Claim(long id, String sheetId, long personId, BigDecimal amount, LocalDate date, String note,
        ClaimStatus status, Integer number, BigDecimal settledDue) {
}
