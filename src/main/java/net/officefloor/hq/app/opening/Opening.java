package net.officefloor.hq.app.opening;

import java.math.BigDecimal;
import net.officefloor.hq.app.period.Period;
import net.officefloor.hq.app.person.Person;

/** One pasted opening balance line, its person and period found on the sheet. */
public record Opening(Person person, Period period, BigDecimal amount) {
}
