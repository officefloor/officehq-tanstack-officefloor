package net.officefloor.hq.app.claim;

import java.util.List;

/**
 * A claim, as the screen reads it: its amount with two decimals, its date as yyyy-MM-dd and its
 * status, its number ("" when it has none), and its due as fixed when it was settled ("" while
 * its due is live), and who opened it ("" until it is opened); whether its person's last
 * statement bound it, and the figure it was bound to ("" when not stated, n/a when it had none);
 * and the number of the claim it corrects ("" when it is no correction); and the date its figures
 * round by (a correction's is the date of the claim it corrects); and its due-by as yyyy-MM-dd, fixed
 * when it was made; and its status log, oldest first, one line per status it has had as
 * {@code <STATUS> by <user> on <date> due=<due>}; and the date it is deferred until as yyyy-MM-dd
 * ("" when it was never deferred; it is deferred only while OPEN and the sheet's today is before it);
 * and how many proofs it has had added; and the number of the claim it was split off ("" when none);
 * and the user whose open editor locks it on the sheet's today ("" when no one's does);
 * and whether it was added while its period was soft-closed; and the last sheet's today it was reminded
 * as yyyy-MM-dd ("" when never); and its external reference ("" when none); and its tags, sorted (none: empty); and the reason it was voided with ("" when none); and its comments, oldest first, as
 * {@code <user> on <date>: <text>}; and the number of the bulk settle batch that settled it ("" when none did);
 * and its lines, oldest first, as {@code <label> <qty> x <unit>}; and its review day, the sheet's today when it
 * went to REVIEW, as yyyy-MM-dd ("" when it never did); and whether a steward waived its late charge; and the
 * late charge its statement bound it to with its figure ("" when none is bound); and how many of its proofs are
 * receipts and how many photos; and whether it is held; and its shares, oldest first;
 * and the user assigned to review it ("" when none).
 */
public record ClaimResponse(String id, String personId, String amount, String date, String note,
        String status, String number, String due, String openedBy, boolean stated,
        String statedDue, String corrects, String roundDate, String dueBy, List<String> log, String deferredUntil,
        int proofs, String splitFrom, String lockedBy, boolean lateEntry, String remindedOn, String ref,
        List<String> tags, String voidReason, List<String> comments, String batch, List<String> lines, String reviewOn, boolean waived,
        String statedLate, int proofReceipts, int proofPhotos, boolean held, List<Shared> shares,
        String reviewUser, String pendingOn) {

    /** A share of the claim as the screen reads it: the holder's person id and their percent, two decimals. */
    public record Shared(String personId, String share) {
    }

    public static ClaimResponse from(Claim claim) {
        return from(claim, List.of(), List.of(), List.of());
    }

    public static ClaimResponse from(Claim claim, List<String> log, List<String> comments, List<String> lines) {
        return from(claim, log, comments, lines, List.of());
    }

    public static ClaimResponse from(Claim claim, List<String> log, List<String> comments, List<String> lines,
            List<ClaimShare> shares) {
        return new ClaimResponse(String.valueOf(claim.getId()), String.valueOf(claim.getPersonId()),
                claim.getAmount().toPlainString(), claim.getDate().toString(), claim.getNote(),
                claim.getStatus().name(),
                claim.getNumber() == null ? "" : claim.getNumber(),
                claim.getSettledDue() == null ? "" : claim.getSettledDue().toPlainString(),
                claim.getOpenedBy() == null ? "" : claim.getOpenedBy(),
                claim.isStated(),
                !claim.isStated() ? "" : claim.getStatedDue() == null ? "n/a" : claim.getStatedDue().toPlainString(),
                claim.getCorrects() == null ? "" : claim.getCorrects(),
                claim.getRoundDate().toString(), claim.getDueBy().toString(), log,
                claim.getDeferredUntil() == null ? "" : claim.getDeferredUntil().toString(),
                claim.getProofs(),
                claim.getSplitFrom() == null ? "" : claim.getSplitFrom(),
                claim.getLockedBy() == null ? "" : claim.getLockedBy(),
                claim.isLateEntry(),
                claim.getRemindedOn() == null ? "" : claim.getRemindedOn().toString(),
                claim.getRef() == null ? "" : claim.getRef(),
                claim.tagList(),
                claim.getVoidReason() == null ? "" : claim.getVoidReason(),
                comments,
                claim.getSettleBatch() == null ? "" : String.valueOf(claim.getSettleBatch()),
                lines,
                claim.getReviewOn() == null ? "" : claim.getReviewOn().toString(),
                claim.isWaived(),
                claim.getStatedLate() == null ? "" : claim.getStatedLate().toPlainString(),
                claim.getProofReceipts(), claim.getProofPhotos(), claim.isHeld(),
                shares.stream().map((s) -> new Shared(String.valueOf(s.personId()), s.share().toPlainString())).toList(),
                claim.getReviewUser() == null ? "" : claim.getReviewUser(),
                claim.getPendingOn() == null ? "" : claim.getPendingOn().toString());
    }
}
