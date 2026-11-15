package net.officefloor.hq.app.claim;

/** Body of an add or edit of a claim. A blank date takes the sheet's today. {@code corrects} is the
 *  number of the SETTLED claim a correction (a negative amount) corrects; {@code ref} its external
 *  reference (blank: none). */
public record ClaimRequest(String amount, String date, String note, String corrects, String ref) {
    public ClaimRequest(String amount, String date, String note) {
        this(amount, date, note, null, null);
    }

    public ClaimRequest(String amount, String date, String note, String corrects) {
        this(amount, date, note, corrects, null);
    }
}
