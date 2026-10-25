package net.officefloor.hq.app.claim;

import java.util.List;

/** A claim as the screen shows it: its amount with two decimals, its date (YYYY-MM-DD) its note, its status, its number (null when it has none)
 * and its due fixed when it was settled (null when it has none), and who opened it (null when never opened);
 * whether its person's last statement binds it, and the figure it binds it to (null: none, or n/a); the number of the
 * claim it corrects (null when it is not a correction); and the date its rounding goes by (YYYY-MM-DD: a correction's is
 * the date of the claim it corrects, else its own); and its due-by (YYYY-MM-DD), fixed when it was created; and its status log, oldest first
 * (<STATUS> by <user> on <date> due=<due>); and the date it is deferred until (YYYY-MM-DD; null when never deferred); and its proofs count, and how many of them are receipts and how many photos; and the number of the claim it was split off from (null when not made by a split); and whether it was added while its period was soft-closed; and the last day it was reminded (YYYY-MM-DD; null when never reminded); and its external reference (null when none); and its tags, sorted (empty when none), and every tag it has ever carried, sorted; and the reason it was voided with (null when none); and its comments, oldest first (<user> on <date>: <text>); and who last edited it (null when never edited); and the number of the bulk settle batch that settled it (null when none); and its lines, in the order added (<label> <qty> x <unit>); and the sheet's today when it last went to REVIEW (YYYY-MM-DD; null when never); and whether its late charge was waived; and whether it is held. */
public record ClaimResponse(long id, long personId, String amount, String date, String note, String status,
        String number, String due, String openedBy,
        boolean stated, String statedDue, String corrects, String roundsOn, String dueBy,
        List<String> log, String deferredUntil, int proofs, int proofsReceipt, int proofsPhoto, String splitFrom, boolean lateEntry, String remindedOn, String ref,
        List<String> tags, List<String> tagsUsed, String voidReason, List<String> comments, String editedBy,
        Integer settleBatch, List<String> lines, String reviewOn, boolean waived, boolean held) {

    public static ClaimResponse from(Claim claim, List<String> log, List<String> comments, List<String> lines) {
        return new ClaimResponse(claim.getId(), claim.getPersonId(), claim.getAmount().setScale(2).toPlainString(),
                claim.getDate().toString(), claim.getNote(), claim.getStatus().name(), claim.getNumber(),
                claim.getDue() == null ? null : claim.getDue().setScale(2).toPlainString(), claim.getOpenedBy(),
                claim.isStated(), claim.getStatedDue() == null ? null : claim.getStatedDue().setScale(2).toPlainString(),
                claim.getCorrects(), claim.getRoundsOn().toString(), claim.getDueBy().toString(), log,
                claim.getDeferredUntil() == null ? null : claim.getDeferredUntil().toString(), claim.getProofs(),
                claim.getProofsReceipt(), claim.getProofsPhoto(),
                claim.getSplitFrom(), claim.isLateEntry(),
                claim.getRemindedOn() == null ? null : claim.getRemindedOn().toString(), claim.getRef(),
                Tags.list(claim.getTags()), Tags.list(claim.getTagsUsed()), claim.getVoidReason(), comments, claim.getEditedBy(),
                claim.getSettleBatch(), lines, claim.getReviewOn() == null ? null : claim.getReviewOn().toString(),
                claim.isWaived(), claim.isHeld());
    }
}
