package net.officefloor.hq.app.claim;

/** Body of a paste: who is making it and the pasted text, one name,amount,date,note line per claim. */
public record PasteRequest(String user, String text) {
}
