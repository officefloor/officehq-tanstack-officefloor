package net.officefloor.hq.app.paste;

import jakarta.validation.constraints.Size;

/** The paste box's text: one claim per line, as name,amount,date,note. */
public record PasteRequest(@Size(max = 200000) String text) {
}
