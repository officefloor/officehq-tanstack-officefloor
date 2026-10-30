package net.officefloor.hq.app.claim;

import java.util.List;

/** amount is shown with two decimals; status is DRAFT, OPEN, PENDING, SETTLED or VOID; number is "" when none;
 *  due is the due fixed when the claim was settled, else (unless PENDING) the figure its person's last statement bound
 *  it to, "" while it is live (or bound to n/a); openedBy is "" when never opened; stated is whether
 *  the last statement bound it; corrects is the number a correction corrects, "" for any other claim;
 *  roundingDate is the date its rounding goes by (a correction's: the date of the claim it corrects);
 *  dueBy is the due-by fixed when the claim was created; log is its status log, oldest first (empty unless listed);
 *  deferredUntil is the date the claim was last deferred until, "" if never; proofs is its proofs count;
 *  splitFrom is the number of the claim a split made it from, "" for any other claim; lockedBy is the user whose
 *  open editor locks it, "" when unlocked; lateEntry is whether it was added while its period was soft-closed;
 *  remindedOn is the last day a reminder was written for it, "" if never; ref is its external reference, "" for none;
 *  tags are its tags, sorted and comma separated, "" for none; voidReason is the reason it was voided with, "" for none;
 *  comments are its comments, oldest first, each "<user> on <date>: <text>" (empty unless listed);
 *  batch is the number of the bulk settle batch that settled it, "" when settled one at a time or never. */
public record ClaimResponse(String id, String personId, String amount, String date, String note, String status,
        String number, String due, String openedBy, boolean stated, String corrects,
        String roundingDate, String dueBy, List<String> log, String deferredUntil, String proofs,
        String splitFrom, String lockedBy, boolean lateEntry, String remindedOn, String ref, String tags,
        String voidReason, List<String> comments, String batch) {

    public static ClaimResponse from(Claim claim) {
        return new ClaimResponse(String.valueOf(claim.getId()), String.valueOf(claim.getPersonId()),
                claim.getAmount().setScale(2).toPlainString(), claim.getDate().toString(), claim.getNote(),
                claim.getStatus().name(), claim.getNumber() == null ? "" : claim.getNumber(),
                fixedDue(claim) == null ? "" : fixedDue(claim).setScale(2).toPlainString(),
                claim.getOpenedBy() == null ? "" : claim.getOpenedBy(), claim.isStated(),
                claim.getCorrects() == null ? "" : claim.getCorrects(),
                claim.getRoundingDate().toString(), claim.getDueBy().toString(), claim.getLog(),
                claim.getDeferredUntil() == null ? "" : claim.getDeferredUntil().toString(),
                String.valueOf(claim.getProofs()),
                claim.getSplitFrom() == null ? "" : claim.getSplitFrom(),
                claim.getLockedBy() == null ? "" : claim.getLockedBy(), claim.isLateEntry(),
                claim.getRemindedOn() == null ? "" : claim.getRemindedOn().toString(),
                claim.getRef() == null ? "" : claim.getRef(),
                claim.getTags() == null ? "" : claim.getTags(),
                claim.getVoidReason() == null ? "" : claim.getVoidReason(),
                claim.getComments().stream().map(ClaimComment::line).toList(),
                claim.getSettleBatch() == null ? "" : String.valueOf(claim.getSettleBatch()));
    }

    /** The due a claim keeps whatever the rates do: fixed when settled, else bound by a statement. */
    public static java.math.BigDecimal fixedDue(Claim claim) {
        if (claim.getSettledDue() != null) return claim.getSettledDue();
        return claim.isBound() ? claim.getStatedDue() : null;
    }

    public static List<ClaimResponse> from(List<Claim> claims) {
        return claims.stream().map(ClaimResponse::from).toList();
    }
}
