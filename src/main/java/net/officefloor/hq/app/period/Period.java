package net.officefloor.hq.app.period;

import java.util.Map;

/** A sheet's period: its name and its from- and to-date (yyyy-mm-dd), both inclusive, whether it is
 *  closed, the due-total fixed when it was closed (two decimals; null while open), and each person's
 *  due-total for it fixed then, by person id (empty while open), and each person's carried figure for it fixed then,
 *  by person id (empty while open), and whether it is soft-closed (not closed, but its
 *  due-total and each person's due-total fixed as when closed; claims whose due-by falls inside it can still be changed), and its cap on each person's OPEN or PENDING claims in it (null for none), and each person's opening balance for it
 *  (the amounts pasted for it, added up), by person id (only those with one), and whether it is locked (a locked period cannot be reopened). */
public record Period(long id, String name, String from, String to, boolean closed, String closedTotal,
        Map<String, String> personTotals, Map<String, String> personCarried, boolean softClosed, Integer cap,
        Map<String, String> personOpenings, boolean locked) {
}
