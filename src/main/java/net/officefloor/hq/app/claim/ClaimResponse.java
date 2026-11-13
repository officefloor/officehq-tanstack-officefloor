package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import java.util.List;

/** A claim as the screen shows it: the amount with two decimals, the date as yyyy-MM-dd, the status by name,
 * the number null unless it has one, the due fixed at settling (two decimals) or null while it is live,
 * the user who opened it or null while it is a draft, whether a statement bound it and the figure it bound
 * it to (two decimals; null: n/a), and the number of the claim a correction corrects (null: not a correction), and its due-by fixed when it was created (yyyy-MM-dd), and its status log, oldest first, and the date it is deferred until (yyyy-MM-dd; null: never deferred), and how many proofs it has been given, and the number of the claim it was split off (null: none), and the user whose open editor locks it (null: not locked), and whether it is a late entry (added while its period was soft-closed), and the last day it was reminded (yyyy-MM-dd; null: never), and its external reference (null: none), and its tags, sorted (none: empty), and the reason it was voided with (null: none), and its comments, oldest first. */
public record ClaimResponse(long id, long personId, String amount, String date, String note, String status, String number,
        String due, String openedBy, boolean stated, String statedDue, String corrects, String dueBy,
        List<LogLine> log, String deferredUntil, int proofs, String splitFrom, String lockedBy,
        boolean lateEntry, String reminded, String ref, List<String> tags, String voidReason,
        List<Comment> comments) {

    /** A line of the status log: the status, the user who gave it, the sheet's today then and the due then. */
    public record LogLine(String status, String by, String date, String due) {
    }

    /** A comment: the user who wrote it, the sheet's today then and its text. */
    public record Comment(String by, String date, String text) {
    }

    public static ClaimResponse from(Claim claim, List<ClaimLogLine> log) {
        return from(claim, log, null, false, null, null, null, null, List.of());
    }

    public static ClaimResponse from(Claim claim, List<ClaimLogLine> log, String lockedBy, boolean lateEntry,
            LocalDate reminded, String ref, String tags, String voidReason, List<ClaimComment> comments) {
        return new ClaimResponse(claim.id(), claim.personId(), claim.amount().toPlainString(),
                claim.date().toString(), claim.note(), claim.status().name(), claim.number(),
                claim.due() == null ? null : claim.due().setScale(2).toPlainString(), claim.openedBy(),
                claim.stated(), claim.statedDue() == null ? null : claim.statedDue().setScale(2).toPlainString(),
                claim.corrects(), claim.dueBy().toString(),
                log.stream().map((l) -> new LogLine(l.status().name(), l.by(), l.date().toString(), l.due())).toList(),
                claim.deferredUntil() == null ? null : claim.deferredUntil().toString(),
                claim.proofs(), claim.splitFrom(), lockedBy, lateEntry,
                reminded == null ? null : reminded.toString(), ref,
                tags == null ? List.of() : List.of(tags.split(",")), voidReason,
                comments.stream().map((c) -> new Comment(c.by(), c.date().toString(), c.text())).toList());
    }
}
