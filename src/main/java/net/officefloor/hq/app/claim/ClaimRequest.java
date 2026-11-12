package net.officefloor.hq.app.claim;

/** Body of a claim add or edit, as typed: the amount, the date (blank for the sheet's today), the
 *  note, who makes the change, the number of the claim a correction corrects (blank for none), and
 *  its external reference (blank for none), and its tags, comma separated (blank for none). */
public record ClaimRequest(String amount, String date, String note, String user, String corrects, String ref, String tags) {

    public ClaimRequest(String amount, String date, String note, String user, String corrects, String ref) {
        this(amount, date, note, user, corrects, ref, null);
    }

    public ClaimRequest(String amount, String date, String note, String user, String corrects) {
        this(amount, date, note, user, corrects, null);
    }
}
