package net.officefloor.hq.app.claim;

/** Why a claim above the note threshold is refused an open while its note is empty. */
final class NoteRequired {
    private NoteRequired() {}

    static final String MESSAGE = "A claim whose due is above the note threshold needs a note first";
}
