package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A person's claim: an amount (0.00 or more, two decimals), a date, a note (may be empty), its
 *  status, its number on the sheet (null while a draft, or once voided) and, once settled, the due fixed
 *  at settling (null before then, or when the person's grade had no rate then: its due stays live), and
 *  who opened it (null while a draft), whether its person's last statement bound it (it counted then) and the
 *  figure it bound it to (null when not bound, or its figure was n/a then: its due stays live). */
public record Claim(long id, String sheetId, long personId, BigDecimal amount, LocalDate date, String note,
        ClaimStatus status, Integer number, BigDecimal settledDue, String openedBy, boolean stated,
        BigDecimal statedDue) {
}
