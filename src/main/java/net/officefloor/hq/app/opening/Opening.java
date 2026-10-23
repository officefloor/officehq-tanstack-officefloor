package net.officefloor.hq.app.opening;

import java.math.BigDecimal;

/** A checked opening: the person (by id, and the name they go by) and the period (as named on the sheet) whose
 *  carried figure the amount adds to. */
public record Opening(long personId, String name, String period, BigDecimal amount) {
}
