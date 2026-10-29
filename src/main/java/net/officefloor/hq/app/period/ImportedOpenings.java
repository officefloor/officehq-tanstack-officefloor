package net.officefloor.hq.app.period;

import java.math.BigDecimal;
import java.util.List;
import net.officefloor.hq.app.person.Person;

/** Every line of a paste of opening balances, checked, in the order typed. */
public record ImportedOpenings(List<Line> lines) {

    /** A line: the person (found by name or alias), the period and the amount to add to their opening balance for it. */
    public record Line(Person person, Period period, BigDecimal amount) {
    }
}
