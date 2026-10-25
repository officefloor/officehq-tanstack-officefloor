package net.officefloor.hq.app.claim;

import java.time.LocalDate;

/** A claim as the screen shows it; `deferred` as of the sheet's today, and `deferredUntil` the date it was last deferred until (null when never deferred), and `splitFrom` the number of the claim it was split off (null when none), and `lateEntry` whether it was added while its period was soft-closed, and `reminded` the last day a reminder was written (null when never), and `ref` its external reference (null when none), and `tags` its tags, sorted (empty when none), and `voidReason` the reason it was voided with (null when none), and `settleBatch` the number of the bulk settle batch that settled it (null when none), and `comments` its comments, oldest first, as `<user> on <date>: <text>`. */
public record ClaimResponse(long id, long personId, String amount, String date, String note, String status,
        String number, String settledDue, String openedBy, boolean stated, String statedDue, String corrects, String dueBy,
        java.util.List<String> log, boolean deferred, String deferredUntil, int proofs,
        String splitFrom, boolean lateEntry, String reminded, String ref,
        java.util.List<String> tags, String voidReason, java.util.List<String> comments,
        Integer settleBatch) {

    /** The claim with its status log, oldest first, as of the sheet's today. */
    public static ClaimResponse from(Claim claim, java.util.List<String> log, LocalDate today) {
        return from(claim, log, java.util.List.of(), today);
    }

    /** The claim with its status log and its comments, each oldest first, as of the sheet's today. */
    public static ClaimResponse from(Claim claim, java.util.List<String> log, java.util.List<String> comments,
            LocalDate today) {
        boolean deferred = claim.isDeferredOn(today);
        return new ClaimResponse(claim.getId(), claim.getPersonId(), claim.getAmount().toPlainString(),
                claim.getDate().toString(), claim.getNote(), claim.getStatus().name(),
                claim.getNumber(), claim.getSettledDue() == null ? null : claim.getSettledDue().toPlainString(),
                claim.getOpenedBy(), claim.isStated(),
                claim.getStatedDue() == null ? null : claim.getStatedDue().toPlainString(), claim.getCorrects(),
                claim.getDueBy().toString(), log, deferred, claim.getDeferredUntil() == null ? null : claim.getDeferredUntil().toString(),
                claim.getProofs(), claim.getSplitFrom(), claim.isLateEntry(),
                claim.getRemindedOn() == null ? null : claim.getRemindedOn().toString(), claim.getRef(),
                claim.getTags() == null ? java.util.List.of() : java.util.List.of(claim.getTags().split(",")),
                claim.getVoidReason(), comments, claim.getSettleBatch());
    }
}
