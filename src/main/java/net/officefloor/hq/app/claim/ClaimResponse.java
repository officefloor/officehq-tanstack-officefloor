package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import java.util.List;

/** A claim as sent to the screen: the amount as text with two decimals, so it is never rounded through a float;
 *  the number is null when the claim has none; due is the due fixed when settled (two decimals), null while it is
 *  live; openedBy is who opened it, null when never opened; stated is whether it is bound to the figure statedDue
 *  (two decimals, null when it had none) by its person's last statement; corrects is the number of the claim a
 *  correction corrects (null for any other claim); roundsOn is the date that decides how its live figures round
 *  (the corrected claim's date for a correction, else its own; see Rounding); dueBy (yyyy-MM-dd) is its due-by, fixed
 *  when it was added; deferred is whether it is deferred on the sheet's today (see ClaimDue#deferred) and deferredUntil
 *  (yyyy-MM-dd) the date it was last deferred until (shown even once that date has passed), null when never deferred; proofs is its proofs count; log is its status log, oldest first: each status it has had, who made the change, the sheet's
 *  today (yyyy-MM-dd) then, and its due as shown at that moment (two decimals, or n/a). */
public record ClaimResponse(long id, String sheetId, long personId, String amount, String date, String note,
        String status, String number, String due, String openedBy, boolean stated, String statedDue, String corrects,
        String roundsOn, String dueBy, boolean deferred, String deferredUntil, int proofs, List<LogLine> log) {

    /** One line of a claim's status log as sent to the screen. */
    public record LogLine(String status, String by, String date, String due) {
    }

    public static ClaimResponse from(Claim claim, ClaimRepository claims, ClaimLogRepository log, LocalDate today) {
        return from(claim, claims, log.of(claim.sheetId(), claim.id()), today);
    }

    public static ClaimResponse from(Claim claim, ClaimRepository claims, List<ClaimLogLine> log, LocalDate today) {
        boolean deferred = ClaimDue.deferred(claim, today);
        return new ClaimResponse(claim.id(), claim.sheetId(), claim.personId(), claim.amount().toPlainString(),
                claim.date().toString(), claim.note(), claim.status().name(), claim.number(),
                claim.due() == null ? null : claim.due().toPlainString(), claim.openedBy(), claim.stated(),
                claim.statedDue() == null ? null : claim.statedDue().toPlainString(), claim.corrects(),
                Rounding.roundsOn(claim, claims).toString(), claim.dueBy().toString(),
                deferred, claim.deferredUntil() == null ? null : claim.deferredUntil().toString(), claim.proofs(),
                log.stream().map((l) -> new LogLine(l.status().name(), l.by(), l.date().toString(), l.due())).toList());
    }
}
