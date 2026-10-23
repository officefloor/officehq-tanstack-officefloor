package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import java.util.List;

/** A claim as the screen shows it: the amount with two decimals, the date as yyyy-mm-dd, the status, its
 *  number (null when it has none) and the due fixed when it was settled (two decimals; null when it is not
 *  settled, or its grade had no rate then, so its due follows the rates), who opened it (null while a draft),
 *  whether its person's last statement bound it and the figure it bound it to (two decimals; null when not
 *  bound, or n/a then), and the number a correction corrects (null when it is not one), and its due-by as yyyy-mm-dd,
 *  and its status log, oldest first (empty when not loaded with it), and the date it is deferred until as yyyy-mm-dd
 *  (null when never deferred) and whether it is deferred on the sheet's today (see Claim.deferredOn; false when
 *  shown without a today), and its proofs count, and the number of the claim it was split from (null when
 *  it was not split off one), and whether it is a late entry, and the last day it was reminded as yyyy-mm-dd (null when never), and its external reference (null when none), and its tags, sorted (empty when none), and its comments, oldest first, as <user> on <date>: <text> (empty when not
 *  loaded with them), and the number of the settle batch that settled it (null when none settled it), and its lines, in the
 *  order added, as <label> <qty> x <unit> (empty when not loaded with them), and its review day as yyyy-mm-dd: the
 *  sheet's today when it went to REVIEW (null when it never did, or not loaded with it). */
public record ClaimResponse(long id, long personId, String amount, String date, String note, String status,
        String number, String settledDue, String openedBy, boolean stated, String statedDue, String corrects, String dueBy,
        List<String> log, String deferredUntil, boolean deferred, int proofs, String splitFrom, boolean lateEntry, String reminded, String ref,
        List<String> tags, List<String> comments, Integer batch, List<String> lines, String reviewOn) {
    public static ClaimResponse from(Claim c) {
        return from(c, List.of(), null);
    }

    public static ClaimResponse from(Claim c, List<String> log, LocalDate today) {
        return from(c, log, today, List.of());
    }

    public static ClaimResponse from(Claim c, List<String> log, LocalDate today, List<String> comments) {
        return from(c, log, today, comments, null);
    }

    public static ClaimResponse from(Claim c, List<String> log, LocalDate today, List<String> comments,
            Integer batch) {
        return from(c, log, today, comments, batch, List.of());
    }

    public static ClaimResponse from(Claim c, List<String> log, LocalDate today, List<String> comments,
            Integer batch, List<String> lines) {
        return from(c, log, today, comments, batch, lines, null);
    }

    public static ClaimResponse from(Claim c, List<String> log, LocalDate today, List<String> comments,
            Integer batch, List<String> lines, LocalDate reviewOn) {
        return new ClaimResponse(c.id(), c.personId(), c.amount().toPlainString(), c.date().toString(), c.note(),
                c.status().name(), c.number(), c.settledDue() != null ? c.settledDue().toPlainString() : null,
                c.openedBy(), c.stated(), c.statedDue() != null ? c.statedDue().toPlainString() : null,
                c.corrects(), c.dueBy().toString(), log,
                c.deferredUntil() != null ? c.deferredUntil().toString() : null, c.deferredOn(today), c.proofs(),
                c.splitFrom(), c.lateEntry(), c.reminded() != null ? c.reminded().toString() : null, c.ref(),
                c.tags() != null ? List.of(c.tags().split(",")) : List.of(), comments, batch, lines,
                reviewOn != null ? reviewOn.toString() : null);
    }
}
