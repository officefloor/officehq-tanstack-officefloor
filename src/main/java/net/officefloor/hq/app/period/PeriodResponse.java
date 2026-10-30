package net.officefloor.hq.app.period;

import java.util.Map;

/** A period: its name and its from and to dates (YYYY-MM-DD, both inclusive); whether it is closed,
 *  the due-total fixed when it was closed ("" while open), and each person's due-total for it fixed
 *  then, by person id (empty while open; a person added later has none); whether it is soft-closed (its figures
 *  fixed as when closed, but its claims still open to change); its cap on each person's OPEN or PENDING claims in it ("" for none); and each person's carried figure for it fixed when
 *  it was closed, by person id (empty while open; a person added later, or a close made before these were kept, has none); and whether it is locked (a locked period cannot be reopened). */
public record PeriodResponse(String name, String from, String to, boolean closed, String closedTotal,
        Map<String, String> closedPersonTotals, boolean softClosed, String cap,
        Map<String, String> closedPersonCarried, boolean locked) {
}
