package net.officefloor.hq.app.period;

import java.math.BigDecimal;

/** A period being closed: who closes it, and (once closed) the due-total it fixed. */
public record PeriodClose(String by, BigDecimal total) {
}
