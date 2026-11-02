package net.officefloor.hq.app.sheet;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import net.officefloor.hq.app.claim.Claims;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.rate.Rates;
import net.officefloor.hq.app.setting.Settings;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;

/**
 * The sheet as it stood at the end of each of its todays: when the sheet's today moves on, what it holds is kept,
 * so a date's state is the one kept when the today first moved from on or before that date to after it.
 */
@Repository
public class SheetStates {

    private final JdbcTemplate jdbc;
    private final JsonMapper json;
    private final People people;
    private final Claims claims;
    private final Rates rates;
    private final Settings settings;

    public SheetStates(JdbcTemplate jdbc, JsonMapper json, People people, Claims claims, Rates rates,
            Settings settings) {
        this.jdbc = jdbc;
        this.json = json;
        this.people = people;
        this.claims = claims;
        this.rates = rates;
        this.settings = settings;
    }

    /** What the sheet holds now. */
    public SheetState now(String sheet) {
        return new SheetState(people.list(sheet), claims.list(sheet), rates.list(sheet),
                settings.history(sheet, Settings.FLOOR), settings.history(sheet, Settings.LATE_PCT),
                settings.history(sheet, Settings.CAP), settings.get(sheet, Settings.POLICY_DATE),
                settings.graces(sheet), settings.history(sheet, Settings.ROUNDING));
    }

    /** Keeps what the sheet holds now, as its today moves from one date to another. */
    public void keep(String sheet, LocalDate from, LocalDate to) {
        jdbc.update("INSERT INTO sheet_state (sheet, from_today, to_today, state) VALUES (?, ?, ?, ?)", sheet,
                Date.valueOf(from), Date.valueOf(to), json.writeValueAsString(now(sheet)));
    }

    /**
     * What the sheet held at the end of the date, before its today: the state kept when the today first moved from on
     * or before it to after it; null when the today was never on or before it.
     */
    public SheetState at(String sheet, LocalDate date) {
        List<String> found = jdbc.queryForList("SELECT state FROM sheet_state WHERE sheet = ? AND from_today <= ?"
                + " AND to_today > ? ORDER BY id LIMIT 1", String.class, sheet, Date.valueOf(date), Date.valueOf(date));
        return found.isEmpty() ? null : json.readValue(found.get(0), SheetState.class);
    }
}
