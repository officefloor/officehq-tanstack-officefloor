package net.officefloor.hq.app.period;

import java.math.BigDecimal;
import java.util.List;

/** The checked lines of an opening balance paste, in order: the person (their id and name) and the
 *  period (its name) each names, and its amount. */
public record OpeningLines(List<Line> lines) {
    public record Line(long personId, String name, String period, BigDecimal amount) {
    }
}
