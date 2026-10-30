package net.officefloor.hq.app.note;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The note store: each sheet's notes, with who added them and the sheet's today when they were added. */
@Repository
public class Notes {

    private final JdbcTemplate jdbc;

    public Notes(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's notes, newest first; with a date, only those added while the sheet's today was on or before it. */
    public List<NoteResponse> list(String sheetId, LocalDate upTo) {
        String sql = "SELECT added_on, added_by, note_text FROM sheet_note WHERE sheet_id = ?"
                + (upTo == null ? "" : " AND added_on <= ?") + " ORDER BY seq DESC";
        Object[] args = upTo == null ? new Object[] { sheetId } : new Object[] { sheetId, Date.valueOf(upTo) };
        return jdbc.query(sql, (rs, i) -> new NoteResponse(rs.getDate(1).toLocalDate().toString(), rs.getString(2),
                rs.getString(3)), args);
    }

    /** Adds the note to the sheet. */
    public void add(String sheetId, String text, String user, LocalDate on) {
        jdbc.update("INSERT INTO sheet_note (sheet_id, note_text, added_by, added_on) VALUES (?, ?, ?, ?)", sheetId, text,
                user, Date.valueOf(on));
    }
}
