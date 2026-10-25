package net.officefloor.hq.app.claim;

public record ClaimResponse(long id, long personId, String amount, String date, String note) {
    public static ClaimResponse from(Claim claim) {
        return new ClaimResponse(claim.getId(), claim.getPersonId(), claim.getAmount().toPlainString(),
                claim.getDate().toString(), claim.getNote());
    }
}
