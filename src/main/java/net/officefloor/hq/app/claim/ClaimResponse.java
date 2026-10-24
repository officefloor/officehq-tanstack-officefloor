package net.officefloor.hq.app.claim;

import java.util.List;

/** A claim as the screen shows it: its amount with two decimals, its date (YYYY-MM-DD) its note, its status, its number (null when it has none)
 * and its due fixed when it was settled (null when it has none), and who opened it (null when never opened);
 * whether its person's last statement binds it, and the figure it binds it to (null: none, or n/a); the number of the
 * claim it corrects (null when it is not a correction); and the date its rounding goes by (YYYY-MM-DD: a correction's is
 * the date of the claim it corrects, else its own); and its due-by (YYYY-MM-DD), fixed when it was created; and its status log, oldest first
 * (<STATUS> by <user> on <date> due=<due>); and the date it is deferred until (YYYY-MM-DD; null when never deferred); and its proofs count; and the number of the claim it was split off from (null when not made by a split); and whether it was added while its period was soft-closed; and the last day it was reminded (YYYY-MM-DD; null when never reminded). */
public record ClaimResponse(long id, long personId, String amount, String date, String note, String status,
        String number, String due, String openedBy,
        boolean stated, String statedDue, String corrects, String roundsOn, String dueBy,
        List<String> log, String deferredUntil, int proofs, String splitFrom, boolean lateEntry, String remindedOn) {

    public static ClaimResponse from(Claim claim, List<String> log) {
        return new ClaimResponse(claim.getId(), claim.getPersonId(), claim.getAmount().setScale(2).toPlainString(),
                claim.getDate().toString(), claim.getNote(), claim.getStatus().name(), claim.getNumber(),
                claim.getDue() == null ? null : claim.getDue().setScale(2).toPlainString(), claim.getOpenedBy(),
                claim.isStated(), claim.getStatedDue() == null ? null : claim.getStatedDue().setScale(2).toPlainString(),
                claim.getCorrects(), claim.getRoundsOn().toString(), claim.getDueBy().toString(), log,
                claim.getDeferredUntil() == null ? null : claim.getDeferredUntil().toString(), claim.getProofs(),
                claim.getSplitFrom(), claim.isLateEntry(),
                claim.getRemindedOn() == null ? null : claim.getRemindedOn().toString());
    }
}
