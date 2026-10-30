package net.officefloor.hq.app.period;

import java.util.Map;

/** A period: its name and its from and to dates (YYYY-MM-DD, both inclusive); whether it is closed,
 *  the due-total fixed when it was closed ("" while open), and each person's due-total for it fixed
 *  then, by person id (empty while open; a person added later has none). */
public record PeriodResponse(String name, String from, String to, boolean closed, String closedTotal,
        Map<String, String> closedPersonTotals) {
}
