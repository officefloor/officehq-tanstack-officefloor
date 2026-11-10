package net.officefloor.hq.app.claim;

import java.util.List;

/**
 * A claim, as the screen reads it: its amount with two decimals, its date as yyyy-MM-dd and its
 * status, its number ("" when it has none), and its due as fixed when it was settled ("" while
 * its due is live), and who opened it ("" until it is opened); whether its person's last
 * statement bound it, and the figure it was bound to ("" when not stated, n/a when it had none);
 * and the number of the claim it corrects ("" when it is no correction); and the date its figures
 * round by (a correction's is the date of the claim it corrects); and its due-by as yyyy-MM-dd, fixed
 * when it was made; and its status log, oldest first, one line per status it has had as
 * {@code <STATUS> by <user> on <date> due=<due>}.
 */
public record ClaimResponse(String id, String personId, String amount, String date, String note,
        String status, String number, String due, String openedBy, boolean stated,
        String statedDue, String corrects, String roundDate, String dueBy, List<String> log) {
    public static ClaimResponse from(Claim claim) {
        return from(claim, List.of());
    }

    public static ClaimResponse from(Claim claim, List<String> log) {
        return new ClaimResponse(String.valueOf(claim.getId()), String.valueOf(claim.getPersonId()),
                claim.getAmount().toPlainString(), claim.getDate().toString(), claim.getNote(),
                claim.getStatus().name(),
                claim.getNumber() == null ? "" : claim.getNumber(),
                claim.getSettledDue() == null ? "" : claim.getSettledDue().toPlainString(),
                claim.getOpenedBy() == null ? "" : claim.getOpenedBy(),
                claim.isStated(),
                !claim.isStated() ? "" : claim.getStatedDue() == null ? "n/a" : claim.getStatedDue().toPlainString(),
                claim.getCorrects() == null ? "" : claim.getCorrects(),
                claim.getRoundDate().toString(), claim.getDueBy().toString(), log);
    }
}
