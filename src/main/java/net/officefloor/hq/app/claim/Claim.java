package net.officefloor.hq.app.claim;

/** A claim of a person on a sheet: its amount with two decimals, its date (ISO yyyy-MM-dd), note and status
 *  (DRAFT, OPEN, SETTLED or VOID), its number on the sheet (null while a draft and once void), and the due fixed when it was settled
 *  (null unless settled with a rate; its due is then worked out live from the rates). */
public record Claim(long id, long personId, String amount, String date, String note, String status,
        Long number, String due) {

    /** The audit field a numbered claim adds at the end of its lines: " number=<n>", or "" when it has none. */
    static String auditNumber(Long number) {
        return number == null ? "" : " number=" + number;
    }
}
