package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A claim made by a person on a sheet. {@code amount} is 0.00 or more, in two decimals. */
public record Claim(long id, String sheetId, long personId, BigDecimal amount, LocalDate date, String note) {

    public Claim with(ClaimInput input, LocalDate date) {
        return new Claim(id, sheetId, personId, input.amount(), date, input.note());
    }
}
