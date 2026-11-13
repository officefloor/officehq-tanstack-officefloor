package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A claim a person has on a sheet. {@code id} is null until the claim is first saved; {@code number}
 * is null unless the claim has been opened (a draft has none, and voiding clears it).
 */
public record Claim(Long id, String sheetId, long personId, BigDecimal amount, LocalDate date, String note,
        ClaimStatus status, Integer number) {

    /** The same claim with another status and number. */
    public Claim withStatus(ClaimStatus next, Integer nextNumber) {
        return new Claim(id, sheetId, personId, amount, date, note, next, nextNumber);
    }

    /** The audit field for the claim's number, " number=<n>", or "" when it has none. */
    public static String auditNumber(Integer number) {
        return number == null ? "" : " number=" + number;
    }
}
