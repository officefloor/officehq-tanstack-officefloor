package net.officefloor.hq.app.claim;

import java.time.LocalDate;

/** A validated claim status change; {@code review} for a steward approving (SETTLED) or rejecting
 *  (OPEN, with its trimmed {@code reason}) a PENDING claim, or reviewing (OPEN) a REVIEW claim; {@code on}
 *  the date a settle is made as of (null for the sheet's today, see {@link ApplyStatus}). */
public record StatusInput(ClaimStatus status, String user, boolean review, String reason, LocalDate on) {

    public StatusInput(ClaimStatus status, String user, boolean review, String reason) {
        this(status, user, review, reason, null);
    }
}
