package net.officefloor.hq.app.note;

import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The notes per sheet, each dated the sheet's today when it was added. Notes are only ever added. */
@Repository
public class NoteStore {

    private final JdbcTemplate jdbc;

    public NoteStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's notes, newest first; with a date, only those added while the sheet's today was on or
     *  before it. */
    public List<NoteResponse> list(String sheetId, LocalDate asOf) {
        String sql = "SELECT id, added_on, user_name, note_text FROM sheet_note WHERE sheet_id = ?"
                + (asOf == null ? "" : " AND added_on <= ?") + " ORDER BY id DESC";
        Object[] args = asOf == null ? new Object[] { sheetId } : new Object[] { sheetId, asOf };
        return jdbc.query(sql, (rs, i) -> new NoteResponse(rs.getLong("id"),
                rs.getObject("added_on", LocalDate.class).toString(), rs.getString("user_name"),
                rs.getString("note_text")), args);
    }

    /** Adds the note, dated the given today; returns it. */
    public NoteResponse add(String sheetId, LocalDate today, String user, String text) {
        jdbc.update("INSERT INTO sheet_note (sheet_id, added_on, user_name, note_text) VALUES (?, ?, ?, ?)",
                sheetId, today, user, text);
        Long id = jdbc.queryForObject("SELECT MAX(id) FROM sheet_note WHERE sheet_id = ?", Long.class, sheetId);
        return new NoteResponse(id == null ? 0 : id, today.toString(), user, text);
    }
}
