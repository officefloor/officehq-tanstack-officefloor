package net.officefloor.hq.app.claim;

/** Body of a paste: lines of name,amount,date,note as typed, and who adds them. */
public record PasteClaimsRequest(String text, String user) {
}
