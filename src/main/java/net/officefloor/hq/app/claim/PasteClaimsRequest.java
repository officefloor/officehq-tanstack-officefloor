package net.officefloor.hq.app.claim;

/** Body of a paste of claims: lines of name,amount,date,note, and who pastes them. */
public record PasteClaimsRequest(String text, String by) {
}
