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
 *  (yyyy-MM-dd) the date it was last deferred until (shown even once that date has passed), null when never deferred; proofs is its proofs count; splitFrom is the number of the claim it was split from (null for any other claim); log is its status log, oldest first: each status it has had, who made the change, the sheet's
 *  today (yyyy-MM-dd) then, and its due as shown at that moment (two decimals, or n/a); lockedBy is who has its editor
 *  open (null when no one does); lateEntry is whether it was added while its period was soft-closed; reminded (yyyy-MM-dd) is the last day a
 *  reminder was written for it, null when none; ref is its external reference, null when none; tags are its tags, sorted (empty when none); comments are its comments in the order written, each with who wrote it and
 *  the sheet's today (yyyy-MM-dd) then; batch is the number of the bulk settle batch that settled it, null when none did. */
public record ClaimResponse(long id, String sheetId, long personId, String amount, String date, String note,
        String status, String number, String due, String openedBy, boolean stated, String statedDue, String corrects,
        String roundsOn, String dueBy, boolean deferred, String deferredUntil, int proofs, String splitFrom, List<LogLine> log,
        String lockedBy, boolean lateEntry, String reminded, String ref, List<String> tags,
        List<Comment> comments, Integer batch) {

    /** One comment on a claim as sent to the screen. */
    public record Comment(String by, String date, String text) {
    }

    /** One line of a claim's status log as sent to the screen. */
    public record LogLine(String status, String by, String date, String due) {
    }

    public static ClaimResponse from(Claim claim, ClaimRepository claims, ClaimLogRepository log, LocalDate today) {
        return from(claim, claims, log.of(claim.sheetId(), claim.id()), today);
    }

    public static ClaimResponse from(Claim claim, ClaimRepository claims, List<ClaimLogLine> log, LocalDate today) {
        return from(claim, claims, log, today, claims.lockedBy(claim.sheetId(), claim.id()),
                claims.lateEntry(claim.sheetId(), claim.id()), claims.remindedOn(claim.sheetId(), claim.id()),
                claims.comments(claim.sheetId(), claim.id()), claims.batchOf(claim.sheetId(), claim.id()));
    }

    public static ClaimResponse from(Claim claim, ClaimRepository claims, List<ClaimLogLine> log, LocalDate today, String lockedBy,
            boolean lateEntry, LocalDate reminded, List<ClaimComment> comments, Integer batch) {
        boolean deferred = ClaimDue.deferred(claim, today);
        return new ClaimResponse(claim.id(), claim.sheetId(), claim.personId(), claim.amount().toPlainString(),
                claim.date().toString(), claim.note(), claim.status().name(), claim.number(),
                claim.due() == null ? null : claim.due().toPlainString(), claim.openedBy(), claim.stated(),
                claim.statedDue() == null ? null : claim.statedDue().toPlainString(), claim.corrects(),
                Rounding.roundsOn(claim, claims).toString(), claim.dueBy().toString(),
                deferred, claim.deferredUntil() == null ? null : claim.deferredUntil().toString(), claim.proofs(), claim.splitFrom(),
                log.stream().map((l) -> new LogLine(l.status().name(), l.by(), l.date().toString(), l.due())).toList(),
                lockedBy, lateEntry, reminded == null ? null : reminded.toString(), claim.ref(),
                claim.tags() == null ? List.of() : List.of(claim.tags().split(",")),
                comments.stream().map((m) -> new Comment(m.by(), m.date().toString(), m.text())).toList(), batch);
    }
}
