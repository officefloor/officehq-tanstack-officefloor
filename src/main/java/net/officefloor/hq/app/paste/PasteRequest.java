package net.officefloor.hq.app.paste;

/** The paste box as submitted: lines of name,amount,date,note. */
public record PasteRequest(String text) {
}
