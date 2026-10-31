package net.officefloor.hq.app.claim;

import java.math.RoundingMode;
import java.util.List;

/** A claim as the screen sees it: the amount with two decimals, the date as yyyy-mm-dd,
 *  the status as DRAFT, OPEN, PENDING, SETTLED or VOID, the number null when the claim has none, the due
 *  fixed at settling with two decimals, null when it has none (its due is worked out live),
 *  who opened it, null while it has not been opened, whether its person's last statement binds it
 *  and the figure it binds it to (two decimals; null when unbound or bound to n/a), and the number
 *  of the claim a correction corrects (null when it is not a correction), and whether its live
 *  figures round half up (else half to even), by the sheet's policy date, and the due-by fixed when it was added (yyyy-mm-dd), and its status log, oldest first (see {@link ClaimLog}),
 *  and whether it is deferred now and the date it was last deferred until (yyyy-mm-dd; null when it never was), and its proofs count, and the number of the claim it was split off (null when it was not), and the user whose open editor locks it (null when none), and whether it was added while its period was soft-closed, and the last day it was reminded (yyyy-mm-dd; null when it never was), and its external reference (null when none), and its tags (sorted, empty when none) and every tag it has ever carried, and the reason it was voided with (null when none). */
public record ClaimResponse(long id, long personId, String amount, String date, String note,
        String status, String number, String due, String openedBy, boolean stated, String statedDue, String corrects, boolean halfUp, String dueBy, List<String> log,
        boolean deferred, String deferredUntil, int proofs, String splitFrom, String lockedBy,
        boolean lateEntry, String remindedOn, String ref, List<String> tags, List<String> tagsUsed, String voidReason) {
    public static ClaimResponse from(Claim claim, Figures figures, List<String> log) {
        return new ClaimResponse(claim.getId(), claim.getPersonId(), claim.getAmount().toPlainString(),
                claim.getDate().toString(), claim.getNote(), claim.getStatus().name(), claim.getNumber(),
                claim.getDue() != null ? claim.getDue().toPlainString() : null, claim.getOpenedBy(),
                claim.isStated(), claim.getStatedDue() != null ? claim.getStatedDue().toPlainString() : null,
                claim.getCorrects(), figures.rounding(claim) == RoundingMode.HALF_UP,
                claim.getDueBy().toString(), log, claim.isDeferred(),
                claim.getDeferredUntil() != null ? claim.getDeferredUntil().toString() : null, claim.getProofs(), claim.getSplitFrom(), claim.getLockedBy(),
                claim.isLateEntry(), claim.getRemindedOn() != null ? claim.getRemindedOn().toString() : null, claim.getRef(),
                split(claim.getTags()), split(claim.getTagsUsed()), claim.getVoidReason());
    }

    private static List<String> split(String tags) {
        return tags == null ? List.of() : List.of(tags.split(","));
    }
}
