package net.officefloor.hq.app.paste;

/**
 * Pasted claims as the screen sends them: one claim per line, as {@code name,amount,date,note}. The
 * date may be blank (the sheet's entry date while it is set, else its today), the note may be absent and may hold commas,
 * and may end {@code " #<reference>"} to give the claim's reference.
 */
public record PasteRequest(String text) {
}
