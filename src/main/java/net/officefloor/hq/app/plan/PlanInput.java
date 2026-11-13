package net.officefloor.hq.app.plan;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A validated plan add: exactly one of {@code every} (days) and {@code day} (of the month) is set; {@code sheet}
 *  the descendant sheet its claims are made on, null for its own. */
public record PlanInput(BigDecimal amount, LocalDate start, Integer every, Integer day, LocalDate end, String user,
        String sheet) {
}
