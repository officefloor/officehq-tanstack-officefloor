package net.officefloor.hq.app.period;

import java.util.Map;

/**
 * A period on a sheet: its name and its from and to date (yyyy-MM-dd), inclusive, whether it is closed, and its
 * due-total fixed when closed (two decimals; null while open), and each person's due-total for it fixed then (by person
 * id, two decimals; empty while open — a person added since has none).
 */
public record Period(String name, String from, String to, boolean closed, String closedTotal,
        Map<Long, String> closedPeople) {
}
