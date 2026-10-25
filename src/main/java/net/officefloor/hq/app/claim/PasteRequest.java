package net.officefloor.hq.app.claim;

/** The paste box as typed: lines of name,amount,date,note, and the signed-in user (`by`) making the change. */
public record PasteRequest(String lines, String by) {
}
