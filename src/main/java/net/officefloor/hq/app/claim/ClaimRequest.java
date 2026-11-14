package net.officefloor.hq.app.claim;

/** Body of an add or edit of a claim. A blank date takes the sheet's today. {@code corrects} is the
 *  number of the SETTLED claim a correction (a negative amount) corrects. */
public record ClaimRequest(String amount, String date, String note, String corrects) {
    public ClaimRequest(String amount, String date, String note) {
        this(amount, date, note, null);
    }
}
