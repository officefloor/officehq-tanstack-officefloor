package net.officefloor.hq.app.claim;

/** A claim of a person on a sheet: its amount with two decimals, its date (ISO yyyy-MM-dd), note and status
 *  (DRAFT, OPEN, SETTLED or VOID), and its number on the sheet (null while a draft and once void). */
public record Claim(long id, long personId, String amount, String date, String note, String status,
        Long number) {

    /** The audit field a numbered claim adds at the end of its lines: " number=<n>", or "" when it has none. */
    static String auditNumber(Long number) {
        return number == null ? "" : " number=" + number;
    }
}
