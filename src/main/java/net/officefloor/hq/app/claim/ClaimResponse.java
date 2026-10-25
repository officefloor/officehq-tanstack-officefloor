package net.officefloor.hq.app.claim;

public record ClaimResponse(long id, long personId, String amount, String date, String note, String status,
        String number, String settledDue, String openedBy, boolean stated, String statedDue, String corrects, String dueBy,
        java.util.List<String> log) {
    public static ClaimResponse from(Claim claim) {
        return from(claim, java.util.List.of());
    }

    /** The claim with its status log, oldest first. */
    public static ClaimResponse from(Claim claim, java.util.List<String> log) {
        return new ClaimResponse(claim.getId(), claim.getPersonId(), claim.getAmount().toPlainString(),
                claim.getDate().toString(), claim.getNote(), claim.getStatus().name(),
                claim.getNumber(), claim.getSettledDue() == null ? null : claim.getSettledDue().toPlainString(),
                claim.getOpenedBy(), claim.isStated(),
                claim.getStatedDue() == null ? null : claim.getStatedDue().toPlainString(), claim.getCorrects(),
                claim.getDueBy().toString(), log);
    }
}
