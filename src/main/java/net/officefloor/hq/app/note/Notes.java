package net.officefloor.hq.app.note;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The notes on each sheet. */
@Repository
public class Notes {

    private final JdbcTemplate jdbc;

    public Notes(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The notes added while the sheet's today was on or before the date, newest first. */
    public List<Note> upTo(String sheet, LocalDate date) {
        return jdbc.query("SELECT id, added_on, added_by, text FROM sheet_note WHERE sheet = ? AND added_on <= ?"
                + " ORDER BY id DESC", (rs, i) -> new Note(rs.getLong("id"), rs.getDate("added_on").toLocalDate().toString(),
                        rs.getString("added_by"), rs.getString("text")), sheet, Date.valueOf(date));
    }

    public void add(String sheet, LocalDate on, String by, String text) {
        jdbc.update("INSERT INTO sheet_note (sheet, added_on, added_by, text) VALUES (?, ?, ?, ?)", sheet,
                Date.valueOf(on), by, text);
    }
}
