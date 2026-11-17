package net.officefloor.hq.app.note;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The stored notes, kept per sheet. */
@Repository
public class NoteRepository {

    private final JdbcTemplate jdbc;

    public NoteRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's notes, newest first; with an as-of date (null: all), only those added on or before it. */
    public List<Note> list(String sheetId, LocalDate asOf) {
        return jdbc.query("SELECT note_text, added_by, added_on FROM sheet_note WHERE sheet_id = ?"
                + (asOf == null ? "" : " AND added_on <= ?") + " ORDER BY id DESC",
                (rs, n) -> new Note(rs.getString("note_text"), rs.getString("added_by"),
                        rs.getObject("added_on", LocalDate.class).toString()),
                asOf == null ? new Object[] { sheetId } : new Object[] { sheetId, Date.valueOf(asOf) });
    }

    /** Adds the note, dated the day given (the sheet's today). */
    public Note add(String sheetId, String text, String by, LocalDate on) {
        jdbc.update("INSERT INTO sheet_note (sheet_id, note_text, added_by, added_on) VALUES (?, ?, ?, ?)", sheetId, text, by,
                Date.valueOf(on));
        return new Note(text, by, on.toString());
    }
}
