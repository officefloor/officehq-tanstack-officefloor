package net.officefloor.hq.app.flag;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/** The store of the people's flags, kept per sheet through the people. */
@Repository
public class Flags {

    private static final RowMapper<Flag> ROW = (rs, n) -> new Flag(rs.getLong("person_id"), rs.getString("reason"),
            rs.getObject("until_date", LocalDate.class));

    private final JdbcTemplate jdbc;

    public Flags(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Every flag on the sheet's people. */
    public List<Flag> list(String sheetId) {
        return jdbc.query("SELECT f.* FROM person_flag f JOIN person p ON p.id = f.person_id WHERE p.sheet_id = ?",
                ROW, sheetId);
    }

    /** The person's flag; null when they have none. */
    public Flag find(long personId) {
        List<Flag> found = jdbc.query("SELECT * FROM person_flag WHERE person_id = ?", ROW, personId);
        return found.isEmpty() ? null : found.get(0);
    }

    /** Whether the person has a flag in force on the date. */
    public boolean flaggedOn(long personId, LocalDate date) {
        Flag flag = find(personId);
        return flag != null && flag.inForceOn(date);
    }

    /** Flag the person, replacing any flag they had. */
    public void set(long personId, String reason, LocalDate until) {
        jdbc.update("DELETE FROM person_flag WHERE person_id = ?", personId);
        jdbc.update("INSERT INTO person_flag (person_id, reason, until_date) VALUES (?, ?, ?)", personId, reason,
                Date.valueOf(until));
    }
}
