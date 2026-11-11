package net.officefloor.hq.app.note;

import java.util.List;

/** The notes on a sheet, newest first. */
public record NotesResponse(List<NoteResponse> notes) {
}
