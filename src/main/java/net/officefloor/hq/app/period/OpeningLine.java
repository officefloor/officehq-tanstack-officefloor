package net.officefloor.hq.app.period;

import java.math.BigDecimal;

/** One checked line of an openings paste: the person's name or alias and the period as typed, and the amount. */
public record OpeningLine(int line, String name, String period, BigDecimal amount) {
}
