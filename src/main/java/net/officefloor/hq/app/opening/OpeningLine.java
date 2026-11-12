package net.officefloor.hq.app.opening;

import java.math.BigDecimal;
import net.officefloor.hq.app.person.Person;

/** A checked line of a paste of opening balances: the person, the period's name and the amount. */
public record OpeningLine(Person person, String period, BigDecimal amount) {
}
