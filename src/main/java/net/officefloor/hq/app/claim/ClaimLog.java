package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.rate.Rates;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.sheet.SheetClock;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/** Writes a line to a claim's status log as it is added and each time its status changes. */
@Service
public class ClaimLog {

    private final JdbcTemplate jdbc;
    private final Claims claims;
    private final People people;
    private final Rates rates;
    private final Settings settings;
    private final SheetClock clock;

    public ClaimLog(JdbcTemplate jdbc, Claims claims, People people, Rates rates, Settings settings, SheetClock clock) {
        this.jdbc = jdbc;
        this.claims = claims;
        this.people = people;
        this.rates = rates;
        this.settings = settings;
        this.clock = clock;
    }

    /** Logs the claim's status as the store now holds it, by the user on the sheet's today, with its due then. */
    public void record(String sheet, String user, long id) {
        Claim claim = claims.find(sheet, id);
        Person person = people.find(sheet, claim.person());
        LocalDate today = clock.today(sheet);
        BigDecimal due = claim.due(rates.list(sheet), person.gradeOn(claim.date()), settings.floor(sheet),
                settings.late(sheet, today));
        jdbc.update("INSERT INTO claim_log (sheet, claim, status, by_user, on_date, due) VALUES (?, ?, ?, ?, ?, ?)",
                sheet, id, claim.status().name(), user, Date.valueOf(today), due == null ? "n/a" : due.toPlainString());
    }
}
