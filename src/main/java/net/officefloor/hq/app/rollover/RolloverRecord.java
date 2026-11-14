package net.officefloor.hq.app.rollover;

import java.time.LocalDate;
import java.util.List;

/** A rollover as stored: who made it on which sheet-today, the periods it generated, and whether it is undone. */
public record RolloverRecord(long id, String sheetId, String fy, String madeBy, LocalDate madeOn, boolean undone,
        List<String> periodNames) {
}
