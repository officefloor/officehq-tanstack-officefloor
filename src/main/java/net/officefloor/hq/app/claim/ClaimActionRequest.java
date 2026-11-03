package net.officefloor.hq.app.claim;

/** A claim's status change: the reason, for a reject, and the date to defer until, for a defer, and the amount to split off, for a split, and the new date, for a redate, and the text, for a comment, and the label, quantity and unit amount, for a line, and the kind, for a proof and the date to settle as of, for a settle (each ignored
 *  by the other actions). */
public record ClaimActionRequest(String reason, String until, String amount, String date, String text, String label, String qty,
        String unit, String kind, String on) {
}
