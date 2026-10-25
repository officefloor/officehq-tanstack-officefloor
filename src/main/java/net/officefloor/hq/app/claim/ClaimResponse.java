package net.officefloor.hq.app.claim;

public record ClaimResponse(long id, long personId, String amount, String date, String note, String status,
        Integer number, String settledDue, String openedBy) {
    public static ClaimResponse from(Claim claim) {
        return new ClaimResponse(claim.getId(), claim.getPersonId(), claim.getAmount().toPlainString(),
                claim.getDate().toString(), claim.getNote(), claim.getStatus().name(),
                claim.getNumber(), claim.getSettledDue() == null ? null : claim.getSettledDue().toPlainString(),
                claim.getOpenedBy());
    }
}
