package net.officefloor.hq.app.note;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class NoteRepository {

    private final JdbcTemplate jdbc;

    public NoteRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public boolean sheetExists(String sheetId) {
        Integer known = jdbc.queryForObject("SELECT COUNT(*) FROM sheet WHERE id = ?", Integer.class, sheetId);
        return known != null && known > 0;
    }

    /** The sheet's notes, newest first. */
    public List<Note> list(String sheetId) {
        return jdbc.query("SELECT id, added_on, added_by, text FROM sheet_note WHERE sheet_id = ? ORDER BY id DESC",
                (rs, i) -> new Note(rs.getLong("id"), rs.getDate("added_on").toLocalDate().toString(),
                        rs.getString("added_by"), rs.getString("text")), sheetId);
    }

    /** Add a note, as of the sheet's today. */
    public void add(String sheetId, LocalDate on, String by, String text) {
        jdbc.update("INSERT INTO sheet_note (sheet_id, added_on, added_by, text) VALUES (?, ?, ?, ?)",
                sheetId, Date.valueOf(on), by, text);
    }
}
