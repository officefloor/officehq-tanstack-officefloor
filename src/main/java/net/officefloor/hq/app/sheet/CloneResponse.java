package net.officefloor.hq.app.sheet;

/** The clone as the screen sees it: the id of the sheet it was cloned to. */
public record CloneResponse(String toSheetId) {

    public static CloneResponse from(ClonedSheet cloned) {
        return new CloneResponse(cloned.toSheetId());
    }
}
