package net.officefloor.hq.app.note;

import jakarta.validation.constraints.Size;

/** A note as the form submits it: its text. */
public record NoteRequest(@Size(max = 4000) String text) {
}
