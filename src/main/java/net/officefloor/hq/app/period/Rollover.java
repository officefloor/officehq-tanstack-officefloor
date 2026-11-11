package net.officefloor.hq.app.period;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** A rollover as stored: the periods it generated and the opening balances it added. */
public record Rollover(long id, String sheetId, String fy, String madeBy, LocalDate madeOn, boolean undone,
        List<String> periods, List<Opening> openings) {

    /** An amount the rollover added to a person's opening balance for a period. */
    public record Opening(String periodName, long personId, BigDecimal amount) {
    }
}
