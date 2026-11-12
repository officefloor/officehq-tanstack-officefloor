package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import java.util.List;

/** A claim as the front end reads it: the amount with two decimals, the date as yyyy-MM-dd,
 *  the status by name, the number as given when opened (1, 2, ... or <period>-<n> / X-<n>; null when it has none), the due fixed when
 *  settled with two decimals (null when it has none, so its due is worked out from the current rate), and who opened it
 *  (null until opened), whether its person's last statement bound it, and the figure it was bound to
 *  (null when not bound, or bound while it had none), and the number of the claim a correction corrects
 *  (null when it is not a correction), and the date its live figures round by (its own, or for a
 *  correction that of the claim it corrects), and the date it is due by, fixed when it was added, and its status log
 *  (see {@link ClaimLogStore}), oldest first. */
public record ClaimResponse(long id, long personId, String amount, String date, String note, String status, String number,
        String due, String openedBy, boolean stated, String statedDue, String corrects, String roundsBy,
        String dueBy, List<String> log) {
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
                c.getStatedDue() == null ? null : c.getStatedDue().setScale(2).toPlainString(),
                c.getCorrects(), roundsBy.toString(), c.getDueBy().toString(), List.of());
    }

    /** The same claim with its status log lines, oldest first. */
    public ClaimResponse withLog(List<String> lines) {
        return new ClaimResponse(id, personId, amount, date, note, status, number, due, openedBy, stated, statedDue,
                corrects, roundsBy, dueBy, lines);
    }
}
