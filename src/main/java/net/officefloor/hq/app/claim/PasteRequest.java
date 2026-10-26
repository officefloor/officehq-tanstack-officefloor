package net.officefloor.hq.app.claim;

/** The body of a paste of claims, as typed: lines of name,amount,date,note (the note may hold commas). */
public record PasteRequest(String text) {
}
