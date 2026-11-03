package net.officefloor.hq.app.note;

import java.util.List;

/** A sheet's notes, newest first. */
public record Notes(List<Note> notes) {
}
