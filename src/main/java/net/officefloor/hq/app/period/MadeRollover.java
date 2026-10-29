package net.officefloor.hq.app.period;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * A rollover as stored: the old fiscal year's name, who made it on which sheet-today, the periods it generated, and
 * the opening balances it set (by period name, then by person id).
 */
public record MadeRollover(long id, String sheetId, String fy, String madeBy, LocalDate madeOn, boolean undone,
        List<String> periods, Map<String, Map<Long, BigDecimal>> openings) {
}
