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
 *  it was not split off one). */
public record ClaimResponse(long id, long personId, String amount, String date, String note, String status,
        String number, String settledDue, String openedBy, boolean stated, String statedDue, String corrects, String dueBy,
        List<String> log, String deferredUntil, boolean deferred, int proofs, String splitFrom) {
    public static ClaimResponse from(Claim c) {
        return from(c, List.of(), null);
    }

    public static ClaimResponse from(Claim c, List<String> log, LocalDate today) {
        return new ClaimResponse(c.id(), c.personId(), c.amount().toPlainString(), c.date().toString(), c.note(),
                c.status().name(), c.number(), c.settledDue() != null ? c.settledDue().toPlainString() : null,
                c.openedBy(), c.stated(), c.statedDue() != null ? c.statedDue().toPlainString() : null,
                c.corrects(), c.dueBy().toString(), log,
                c.deferredUntil() != null ? c.deferredUntil().toString() : null, c.deferredOn(today), c.proofs(),
                c.splitFrom());
    }
}
