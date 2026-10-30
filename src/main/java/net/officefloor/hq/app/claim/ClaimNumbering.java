package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import net.officefloor.hq.app.period.Periods;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.sheet.Sheets;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Gives an opening claim its number. While the sheet's period numbering from is set and the sheet's
 * today is on or after it, the number counts from 1 within the claim's period: {@code <period>-<n>},
 * or {@code X-<n>} in its own sequence for a claim in no period. Otherwise it is the sheet-wide number,
 * whose sequence only the claims numbered sheet-wide advance.
 */
@Repository
public class ClaimNumbering {

    /** The prefix of the sequence of claims whose due-by is in no period. */
    public static final String NO_PERIOD = "X";

    private final JdbcTemplate jdbc;

    private final Sheets sheets;

    private final Settings settings;

    private final Periods periods;

    public ClaimNumbering(JdbcTemplate jdbc, Sheets sheets, Settings settings, Periods periods) {
        this.jdbc = jdbc;
        this.sheets = sheets;
        this.settings = settings;
        this.periods = periods;
    }

    /** Whether claims are numbered within their period now: the period numbering from is set and the sheet's today is on or after it. */
    public boolean perPeriod(String sheetId) {
        LocalDate from = settings.periodNumberingFrom(sheetId);
        return from != null && !sheets.today(sheetId).isBefore(from);
    }

    /** The next number for a claim dated on the date, opened now. */
    public String next(String sheetId, LocalDate claimDate) {
        if (!perPeriod(sheetId)) {
            return String.valueOf(sheets.nextClaimNumber(sheetId));
        }
        String period = periods.nameOn(sheetId, claimDate);
        String key = period == null ? "" : period;
        // Lock the sheet's row so claims opened at the same moment get distinct numbers.
        jdbc.query("SELECT id FROM sheet WHERE id = ? FOR UPDATE", (rs, i) -> rs.getString(1), sheetId);
        jdbc.update("MERGE INTO period_claim_number (sheet_id, period_name, last_number) KEY (sheet_id, period_name)"
                + " VALUES (?, ?, COALESCE((SELECT last_number FROM period_claim_number WHERE sheet_id = ? AND period_name = ?), 0) + 1)",
                sheetId, key, sheetId, key);
        Long n = jdbc.queryForObject("SELECT last_number FROM period_claim_number WHERE sheet_id = ? AND period_name = ?",
                Long.class, sheetId, key);
        return (period == null ? NO_PERIOD : period) + "-" + n;
    }
}
