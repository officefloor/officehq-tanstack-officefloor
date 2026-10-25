package net.officefloor.hq.app.claim;

import java.time.LocalDate;

/** A claim as the screen shows it; `deferred` and `deferredUntil` (null when not deferred) as of the sheet's today. */
public record ClaimResponse(long id, long personId, String amount, String date, String note, String status,
        String number, String settledDue, String openedBy, boolean stated, String statedDue, String corrects, String dueBy,
        java.util.List<String> log, boolean deferred, String deferredUntil) {

    /** The claim with its status log, oldest first, as of the sheet's today. */
    public static ClaimResponse from(Claim claim, java.util.List<String> log, LocalDate today) {
        boolean deferred = claim.isDeferredOn(today);
        return new ClaimResponse(claim.getId(), claim.getPersonId(), claim.getAmount().toPlainString(),
                claim.getDate().toString(), claim.getNote(), claim.getStatus().name(),
                claim.getNumber(), claim.getSettledDue() == null ? null : claim.getSettledDue().toPlainString(),
                claim.getOpenedBy(), claim.isStated(),
                claim.getStatedDue() == null ? null : claim.getStatedDue().toPlainString(), claim.getCorrects(),
                claim.getDueBy().toString(), log, deferred, deferred ? claim.getDeferredUntil().toString() : null);
    }
}
