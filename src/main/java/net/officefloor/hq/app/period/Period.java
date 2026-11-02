package net.officefloor.hq.app.period;

import java.util.Map;

/**
 * A period on a sheet: its name and its from and to date (yyyy-MM-dd), inclusive, whether it is closed, and its
 * due-total fixed when closed (two decimals; null while open), and each person's due-total for it fixed then (by person
 * id, two decimals; empty while open — a person added since has none), and whether it is soft-closed instead: its
 * figures fixed the same way, but claims due inside it may still be added, edited and change status; and the most OPEN
 * or PENDING claims a person may have in it (null for no cap); and each person's carried figure for it fixed when it was
 * (soft-)closed (by person id, two decimals; empty while open).
 */
public record Period(String name, String from, String to, boolean closed, String closedTotal,
        Map<Long, String> closedPeople, boolean soft, Integer cap, Map<Long, String> closedCarried) {

    /** Whether its figures are fixed: closed or soft-closed. */
    public boolean fixed() {
        return closed || soft;
    }
}
