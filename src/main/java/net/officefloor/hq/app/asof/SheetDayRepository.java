package net.officefloor.hq.app.asof;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.setting.SettingRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;

/**
 * The sheet as it stood each time its today was changed, so its figures can be read as they stood at the end of an
 * earlier date: every change made while the sheet's today was on or before that date counts, every later one does
 * not.
 */
@Repository
public class SheetDayRepository {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final JdbcTemplate jdbc;
    private final PersonRepository people;
    private final ClaimRepository claims;
    private final RateRepository rates;
    private final SettingRepository settings;

    public SheetDayRepository(JdbcTemplate jdbc, PersonRepository people, ClaimRepository claims, RateRepository rates,
            SettingRepository settings) {
        this.jdbc = jdbc;
        this.people = people;
        this.claims = claims;
        this.rates = rates;
        this.settings = settings;
    }

    /** Keep the sheet as it stands now, as its today moves from {@code from} to {@code to}. */
    public void record(String sheetId, LocalDate from, LocalDate to) {
        SheetState now = new SheetState(people.findAll(sheetId), claims.findAll(sheetId), rates.findAll(sheetId),
                settings.dated(sheetId).history(), settings.policyDate(sheetId));
        jdbc.update("INSERT INTO sheet_day (sheet_id, from_today, to_today, state) VALUES (?, ?, ?, ?)", sheetId,
                Date.valueOf(from), Date.valueOf(to), JSON.writeValueAsString(now));
    }

    /**
     * The sheet as it stood at the end of the date, or null when that is as it stands now (its today has not moved
     * past the date since it was last on or before it). Before the sheet was first loaded nothing was on it.
     */
    public SheetState asOf(String sheetId, LocalDate date) {
        List<LocalDate> started = jdbc.query("SELECT started FROM sheet WHERE id = ?",
                (rs, n) -> rs.getObject("started", LocalDate.class), sheetId);
        boolean empty = !started.isEmpty() && started.get(0) != null && started.get(0).isAfter(date);
        Long kept = null;
        for (Map<String, Object> day : jdbc.queryForList(
                "SELECT id, from_today, to_today FROM sheet_day WHERE sheet_id = ? ORDER BY id", sheetId)) {
            LocalDate from = ((Date) day.get("from_today")).toLocalDate();
            LocalDate to = ((Date) day.get("to_today")).toLocalDate();
            if (!to.isAfter(date)) {
                empty = false;
                kept = null;
            } else if (!from.isAfter(date)) {
                empty = false;
                kept = ((Number) day.get("id")).longValue();
            }
        }
        if (kept != null) {
            String state = jdbc.queryForObject("SELECT state FROM sheet_day WHERE id = ?", String.class, kept);
            return JSON.readValue(state, SheetState.class);
        }
        return empty ? SheetState.EMPTY : null;
    }
}
