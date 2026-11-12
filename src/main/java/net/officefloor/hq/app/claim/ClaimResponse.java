package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import java.util.List;

/** A claim as the front end reads it: the amount with two decimals, the date as yyyy-MM-dd,
 *  the status by name, the number as given when opened (1, 2, ... or <period>-<n> / X-<n>; null when it has none), the due fixed when
 *  settled with two decimals (null when it has none, so its due is worked out from the current rate), and who opened it
 *  (null until opened), whether its person's last statement bound it, and the figure it was bound to
 *  (null when not bound, or bound while it had none), whether it was bound to n/a (its rate had ended), and the number of the claim a correction corrects
 *  (null when it is not a correction), and the date its live figures round by (its own, or for a
 *  correction that of the claim it corrects), and the date it is due by, fixed when it was added, and its status log
 *  (see {@link ClaimLogStore}), oldest first, and whether it is deferred on the sheet's today (see
 *  {@link Claim#isDeferredOn}) and the date it was deferred until (null when never deferred), and how many proofs it has, and the number of the claim it was
 *  split off (null when it was not split off another), and whether it is a late entry (added while its period was soft-closed),
 *  and the last date it was reminded on (null when never reminded), and its external reference (null when it has none), and its tags, sorted (empty when it has none),
 *  and the reason it was voided with (null when none), and its comments (see {@link ClaimCommentStore}), oldest first. */
public record ClaimResponse(long id, long personId, String amount, String date, String note, String status, String number,
        String due, String openedBy, boolean stated, String statedDue, boolean statedNa, String corrects, String roundsBy,
        String dueBy, List<String> log, boolean deferred, String deferredUntil, int proofs,
        String splitFrom, boolean lateEntry, String remindedOn, String ref, List<String> tags, String voidReason,
        List<String> comments) {
    public static ClaimResponse from(Claim c) {
        return from(c, c.getDate());
    }

    public static ClaimResponse from(Claim c, Rounding rounding) {
        return from(c, rounding.date(c));
    }

    public static ClaimResponse from(Claim c, LocalDate roundsBy) {
        return new ClaimResponse(c.getId(), c.getPersonId(), c.getAmount().toPlainString(), c.getDate().toString(),
                c.getNote(), c.getStatus().name(), c.getNumber(),
                c.getSettledDue() == null ? null : c.getSettledDue().setScale(2).toPlainString(),
                c.getOpenedBy(), c.isStated(),
                c.getStatedDue() == null ? null : c.getStatedDue().setScale(2).toPlainString(), c.isStatedNa(),
                c.getCorrects(), roundsBy.toString(), c.getDueBy().toString(), List.of(),
                false, c.getDeferredUntil() == null ? null : c.getDeferredUntil().toString(), c.getProofs(),
                c.getSplitFrom(), c.isLateEntry(), c.getRemindedOn() == null ? null : c.getRemindedOn().toString(), c.getRef(),
                Tags.list(c.getTags()), c.getVoidReason(), List.of());
    }

    /** The same claim with its status log lines, oldest first. */
    public ClaimResponse withLog(List<String> lines) {
        return new ClaimResponse(id, personId, amount, date, note, status, number, due, openedBy, stated, statedDue,
                statedNa, corrects, roundsBy, dueBy, lines, deferred, deferredUntil, proofs, splitFrom, lateEntry, remindedOn, ref, tags, voidReason, comments);
    }

    /** The same claim with its comment lines, oldest first. */
    public ClaimResponse withComments(List<String> lines) {
        return new ClaimResponse(id, personId, amount, date, note, status, number, due, openedBy, stated, statedDue,
                statedNa, corrects, roundsBy, dueBy, log, deferred, deferredUntil, proofs, splitFrom, lateEntry, remindedOn, ref, tags, voidReason, lines);
    }

    /** The same claim, deferred when OPEN and deferred until a date after the sheet's {@code today}. */
    public ClaimResponse on(LocalDate today) {
        boolean now = ClaimStatus.OPEN.name().equals(status) && deferredUntil != null && today != null
                && today.toString().compareTo(deferredUntil) < 0;
        return new ClaimResponse(id, personId, amount, date, note, status, number, due, openedBy, stated, statedDue,
                statedNa, corrects, roundsBy, dueBy, log, now, deferredUntil, proofs, splitFrom, lateEntry, remindedOn, ref, tags, voidReason, comments);
    }
}
