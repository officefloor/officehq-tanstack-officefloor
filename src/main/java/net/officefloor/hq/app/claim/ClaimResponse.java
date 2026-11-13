package net.officefloor.hq.app.claim;

import java.util.List;

/** A claim as the screen shows it: the amount with two decimals, the date as yyyy-MM-dd, the status by name,
 * the number null unless it has one, the due fixed at settling (two decimals) or null while it is live,
 * the user who opened it or null while it is a draft, whether a statement bound it and the figure it bound
 * it to (two decimals; null: n/a), and the number of the claim a correction corrects (null: not a correction), and its due-by fixed when it was created (yyyy-MM-dd), and its status log, oldest first. */
public record ClaimResponse(long id, long personId, String amount, String date, String note, String status, String number,
        String due, String openedBy, boolean stated, String statedDue, String corrects, String dueBy,
        List<LogLine> log) {

    /** A line of the status log: the status, the user who gave it, the sheet's today then and the due then. */
    public record LogLine(String status, String by, String date, String due) {
    }

    public static ClaimResponse from(Claim claim, List<ClaimLogLine> log) {
        return new ClaimResponse(claim.id(), claim.personId(), claim.amount().toPlainString(),
                claim.date().toString(), claim.note(), claim.status().name(), claim.number(),
                claim.due() == null ? null : claim.due().setScale(2).toPlainString(), claim.openedBy(),
                claim.stated(), claim.statedDue() == null ? null : claim.statedDue().setScale(2).toPlainString(),
                claim.corrects(), claim.dueBy().toString(),
                log.stream().map((l) -> new LogLine(l.status().name(), l.by(), l.date().toString(), l.due())).toList());
    }
}
