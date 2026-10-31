package net.officefloor.hq.app.plan;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A person's plan: a claim of `amount` on the start date and every `every` days after, up to the
 *  end date; `generated` is how many claims it has made so far. */
public record Plan(long id, String sheetId, long personId, BigDecimal amount, LocalDate start, int every,
        LocalDate end, int generated) {
}
