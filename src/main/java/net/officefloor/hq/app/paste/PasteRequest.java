package net.officefloor.hq.app.paste;

/** Body of POST /api/sheets/{sheet}/claims/paste: lines of name,amount,date,note. */
public record PasteRequest(String text) {
}
