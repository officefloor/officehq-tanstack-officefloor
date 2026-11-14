package net.officefloor.hq.app.opening;

import java.math.BigDecimal;
import net.officefloor.hq.app.period.Period;
import net.officefloor.hq.app.person.Person;

/** One opening line: as typed (name and period, amount checked), then with its person and period found. */
public record PastedOpening(String name, String periodName, BigDecimal amount, Person person, Period period) {
}
