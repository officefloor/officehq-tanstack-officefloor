package net.officefloor.hq.app.claim;

import java.util.List;

/** A claim as shown: its amount with two decimals, its date as yyyy-mm-dd, its status, its number
 *  (null when it has none) and, once settled, its fixed due with two decimals (null while live, or when
 *  no rate applied at settling) and who opened it (null while a draft), and the
 *  number a correction corrects (null for any other claim), and its period number when it was numbered
 *  within its period (null otherwise), and its due-by as yyyy-mm-dd, and its status log, oldest first. */
public record ClaimResponse(long id, long personId, String amount, String date, String note, String status,
        Integer number, String settledDue, String openedBy, Integer corrects, String periodNumber, String dueBy,
        List<LogLine> log) {

    /** A line of the status log: its status, who made the change, the sheet's today then (yyyy-mm-dd) and the
     *  claim's due then with two decimals (null when no rate applied). */
    public record LogLine(String status, String by, String date, String due) {
        static LogLine from(ClaimLogEntry entry) {
            return new LogLine(entry.getStatus().name(), entry.getBy(), entry.getDate().toString(),
                    entry.getDue() == null ? null : entry.getDue().setScale(2).toPlainString());
        }
    }

    /** The claim without its status log. */
    public static ClaimResponse from(Claim claim) {
        return from(claim, List.of());
    }

    /** The claim with its status log (its lines, oldest first). */
    public static ClaimResponse from(Claim claim, List<ClaimLogEntry> log) {
        return new ClaimResponse(claim.getId(), claim.getPersonId(), claim.getAmount().setScale(2).toPlainString(),
                claim.getDate().toString(), claim.getNote(), claim.getStatus().name(), claim.getNumber(),
                claim.getSettledDue() == null ? null : claim.getSettledDue().setScale(2).toPlainString(),
                claim.getOpenedBy(), claim.getCorrects(), claim.getPeriodNumber(),
                claim.getDueBy().toString(), log.stream().map(LogLine::from).toList());
    }
}
