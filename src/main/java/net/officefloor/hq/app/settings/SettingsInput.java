package net.officefloor.hq.app.settings;

import java.math.BigDecimal;

/** A validated settings form: which settings it saves, the clerk limit scaled to two decimals (null for
 *  no limit), the floor scaled to two decimals and the terms in days. */
public record SettingsInput(boolean setsClerkLimit, BigDecimal clerkLimit, boolean setsFloor, BigDecimal floor, boolean setsTerms,
        Integer terms) {
}
