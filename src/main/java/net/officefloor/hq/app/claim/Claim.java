package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A claim a person has on a sheet. {@code id} is null until the claim is first saved. */
public record Claim(Long id, String sheetId, long personId, BigDecimal amount, LocalDate date, String note,
        ClaimStatus status) {

    /** The same claim with another status. */
    public Claim withStatus(ClaimStatus next) {
        return new Claim(id, sheetId, personId, amount, date, note, next);
    }
}
