package net.officefloor.hq.app.flag;

import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Each person's flag, per sheet: a reason and the date it runs until; flagging again replaces it. */
@Repository
public class FlagRepository {

    private final JdbcTemplate jdbc;

    public FlagRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Flag the person with the reason until the date, replacing any flag they had. */
    public void flag(String sheetId, long personId, String reason, LocalDate until) {
        jdbc.update("MERGE INTO person_flag KEY (sheet_id, person_id) VALUES (?, ?, ?, ?)", sheetId, personId, reason,
                until);
    }

    /** The reason the person is flagged for while the sheet's today is on or before its until date; null when not. */
    public String inForce(String sheetId, long personId) {
        List<String> found = jdbc.queryForList("SELECT f.reason FROM person_flag f JOIN sheet s ON s.id = f.sheet_id "
                + "WHERE f.sheet_id = ? AND f.person_id = ? AND s.today <= f.until_date", String.class, sheetId,
                personId);
        return found.isEmpty() ? null : found.get(0);
    }

    /** The flags in force on the sheet's today, by person id. */
    public List<FlagResponse> allInForce(String sheetId) {
        return jdbc.query("SELECT f.* FROM person_flag f JOIN sheet s ON s.id = f.sheet_id "
                + "WHERE f.sheet_id = ? AND s.today <= f.until_date ORDER BY f.person_id",
                (rs, i) -> new FlagResponse(rs.getLong("person_id"), rs.getString("reason"),
                        rs.getObject("until_date", LocalDate.class).toString()), sheetId);
    }
}
