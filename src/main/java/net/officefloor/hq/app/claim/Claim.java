package net.officefloor.hq.app.claim;

/** A claim as the screen shows it: amount with two decimals, date (ISO yyyy-MM-dd), note and status. */
public record Claim(long id, long person, String amount, String date, String note, ClaimStatus status) {

    /** The audit fields of this claim for the named person, as {@link ClaimInput#auditFields}. */
    public String auditFields(String person) {
        return "person=" + person + " amount=" + amount + " date=" + date;
    }
}
