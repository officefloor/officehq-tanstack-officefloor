package net.officefloor.hq.app.claim;

/** A claim as the screen sees it: the amount with two decimals, the date as yyyy-MM-dd, the status by name, the number (null when it has none), the due fixed on settling (null when it has none), who opened it (null when never opened), whether its person's last statement bound it, the figure it bound (null when it has none) the number a correction corrects (null for any other claim) and the date it is due by, as yyyy-MM-dd, its status log, oldest first (empty when not listed with it), and the date it was deferred to, as yyyy-MM-dd (null when never deferred), and how many proofs it has been given, and the number of the claim it was split off (null for any other claim), and the user whose open editor locks it (null when not locked), and whether it was added while its period was soft-closed, and the last day it was reminded, as yyyy-MM-dd (null when never reminded), and its external reference (null when it has none), and its tags, sorted (empty for none), and its comments, oldest first, as "user on date: text" (empty when not listed with them), and the number of the settle batch that settled it (null when none, or not listed with it), and its lines, in the order added, as "label qty x unit" (empty when not listed with them), and the sheet's today it last went to REVIEW, as yyyy-MM-dd (null when it never did, or not listed with it). */
public record ClaimResponse(long id, long personId, String amount, String date, String note, String status,
        String number, String settledDue, String openedBy, boolean stated, String statedDue,
        String corrects, String dueBy, java.util.List<String> log, String deferredUntil, int proofs,
        String splitFrom, String lockedBy, boolean lateEntry, String remindedOn, String ref, java.util.List<String> tags,
        java.util.List<String> comments, Integer batch, java.util.List<String> lines, String reviewOn) {

    public static ClaimResponse from(Claim claim) {
        return from(claim, java.util.List.of());
    }

    public static ClaimResponse from(Claim claim, java.util.List<String> log) {
        return new ClaimResponse(claim.id(), claim.personId(), claim.amount().toPlainString(),
                claim.date().toString(), claim.note(), claim.status().name(), claim.number(),
                claim.settledDue() == null ? null : claim.settledDue().toPlainString(), claim.openedBy(), claim.stated(),
                claim.statedDue() == null ? null : claim.statedDue().toPlainString(), claim.corrects(),
                claim.dueBy().toString(), log,
                claim.deferredUntil() == null ? null : claim.deferredUntil().toString(), claim.proofs(),
                claim.splitFrom(), null, claim.lateEntry(),
                claim.remindedOn() == null ? null : claim.remindedOn().toString(), claim.ref(), claim.tags(),
                java.util.List.of(), null, java.util.List.of(), null);
    }

    /** The claim as locked by {@code user} (null: not locked). */
    public ClaimResponse withLockedBy(String user) {
        return new ClaimResponse(id, personId, amount, date, note, status, number, settledDue, openedBy, stated,
                statedDue, corrects, dueBy, log, deferredUntil, proofs, splitFrom, user, lateEntry, remindedOn, ref, tags, comments, batch, lines, reviewOn);
    }

    /** The claim with its comments, oldest first. */
    public ClaimResponse withComments(java.util.List<String> comments) {
        return new ClaimResponse(id, personId, amount, date, note, status, number, settledDue, openedBy, stated,
                statedDue, corrects, dueBy, log, deferredUntil, proofs, splitFrom, lockedBy, lateEntry, remindedOn, ref, tags,
                comments, batch, lines, reviewOn);
    }

    /** The claim with the number of the settle batch that settled it (null: none). */
    public ClaimResponse withBatch(Integer batch) {
        return new ClaimResponse(id, personId, amount, date, note, status, number, settledDue, openedBy, stated,
                statedDue, corrects, dueBy, log, deferredUntil, proofs, splitFrom, lockedBy, lateEntry, remindedOn, ref, tags,
                comments, batch, lines, reviewOn);
    }

    /** The claim with its lines, in the order added, each "label qty x unit". */
    public ClaimResponse withLines(java.util.List<String> lines) {
        return new ClaimResponse(id, personId, amount, date, note, status, number, settledDue, openedBy, stated,
                statedDue, corrects, dueBy, log, deferredUntil, proofs, splitFrom, lockedBy, lateEntry, remindedOn, ref, tags,
                comments, batch, lines, reviewOn);
    }

    /** The claim with the sheet's today it last went to REVIEW (null: never). */
    public ClaimResponse withReviewOn(java.time.LocalDate on) {
        return new ClaimResponse(id, personId, amount, date, note, status, number, settledDue, openedBy, stated,
                statedDue, corrects, dueBy, log, deferredUntil, proofs, splitFrom, lockedBy, lateEntry, remindedOn, ref, tags,
                comments, batch, lines, on == null ? null : on.toString());
    }
}
