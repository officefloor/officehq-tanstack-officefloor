package net.officefloor.hq.app.sheet;

import java.time.LocalDate;

/**
 * The preview date of a read: a screen previewing the sheet as if its today were a date sends it as the
 * {@code preview} query parameter of every read (never of a change: every change uses the sheet's real today). The
 * sheet's current records are read as they are; only the date that stands in for today in every figure changes.
 */
public final class Preview {

    private Preview() {
    }

    /** The preview date given (yyyy-MM-dd), or null when none (or none readable) was. */
    public static LocalDate parse(String preview) {
        return AsOf.parse(preview);
    }

    /** The date a read's figures take as the sheet's today: the as-of date, else the preview date, else its today. */
    public static LocalDate today(String sheetId, LocalDate asOf, String preview, SheetRepository sheets) {
        if (asOf != null) {
            return asOf;
        }
        LocalDate shown = parse(preview);
        return shown != null ? shown : sheets.today(sheetId);
    }
}
