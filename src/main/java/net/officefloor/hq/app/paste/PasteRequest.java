package net.officefloor.hq.app.paste;

/** Body of a paste: lines of name,amount,date,note (the note may hold commas; the date may be blank
 *  for the sheet's today; the note may be absent). */
public record PasteRequest(String text) {
}
