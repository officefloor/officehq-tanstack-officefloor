package net.officefloor.hq.app.opening;

import java.math.BigDecimal;
import net.officefloor.hq.app.period.Period;
import net.officefloor.hq.app.person.Person;

/** One checked line of a paste of opening balances. */
public record OpeningLine(Person person, Period period, BigDecimal amount) {
}
