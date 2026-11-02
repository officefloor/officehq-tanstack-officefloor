package net.officefloor.hq.app.period;

import java.math.BigDecimal;
import java.util.List;

/** The checked lines of an opening-balance paste, in order: the person (by id and name), the period and the amount. */
public record ImportedOpenings(List<Line> lines) {

    public record Line(long person, String name, String period, BigDecimal amount) {
    }
}
