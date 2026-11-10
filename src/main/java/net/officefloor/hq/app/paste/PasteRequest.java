package net.officefloor.hq.app.paste;

/**
 * Pasted claims as the screen sends them: one claim per line, as {@code name,amount,date,note}. The
 * date may be blank (the sheet's today), the note may be absent and may hold commas.
 */
public record PasteRequest(String text) {
}
