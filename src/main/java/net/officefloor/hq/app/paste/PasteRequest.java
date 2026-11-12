package net.officefloor.hq.app.paste;

/** Body of a paste of claims, as typed: lines of name,amount,date,note and who makes the change. */
public record PasteRequest(String text, String user) {
}
