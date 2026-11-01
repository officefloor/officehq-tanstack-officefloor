package net.officefloor.hq.app.claim;

/**
 * A claim as the screen shows it: amount with two decimals, date (ISO yyyy-MM-dd), note, status and its number on
 * the sheet (given when opened; null while a draft and once voided), and its due fixed when settled (two decimals;
 * null until settled, and when the grade had no rate then), and the user who opened it (null until opened).
 */
public record Claim(long id, long person, String amount, String date, String note, ClaimStatus status,
        Integer number, String settledDue, String openedBy) {

    /** The audit fields of this claim for the named person, as {@link ClaimInput#auditFields}. */
    public String auditFields(String person) {
        return "person=" + person + " amount=" + amount + " date=" + date;
    }

    /** This claim, numbered: its audit fields end with number=<number> when it has one. */
    public Claim withNumber(Integer number) {
        return new Claim(id, person, amount, date, note, status, number, settledDue, openedBy);
    }

    /** This claim, settled with its due fixed (null when its grade had no rate). */
    public Claim withSettledDue(String settledDue) {
        return new Claim(id, person, amount, date, note, status, number, settledDue, openedBy);
    }

    /** The audit fields of a status change: {@link #auditFields} then number=<number> when it has one. */
    public String statusAuditFields(String person) {
        return auditFields(person) + (number == null ? "" : " number=" + number);
    }
}
