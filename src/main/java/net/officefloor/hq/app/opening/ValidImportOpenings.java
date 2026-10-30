package net.officefloor.hq.app.opening;

import java.math.BigDecimal;
import java.util.List;

/** An import whose every line is well formed, in the order pasted. */
public record ValidImportOpenings(List<Line> lines, String user) {

    /** One line: the name (or alias) of a person, a period name and an amount of 0.00 or more. */
    public record Line(String name, String period, BigDecimal amount) {
    }
}
