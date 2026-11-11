package net.officefloor.hq.app.note;

import java.sql.Date;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The notes store, kept per sheet. */
@Repository
public class NoteRepository {

    private final JdbcTemplate jdbc;

    public NoteRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's notes, newest first. */
    public List<Note> findAll(String sheetId) {
        return jdbc.query("SELECT sheet_id, added_on, added_by, text FROM sheet_note WHERE sheet_id = ? ORDER BY id DESC",
                (rs, n) -> new Note(rs.getString("sheet_id"), rs.getDate("added_on").toLocalDate(),
                        rs.getString("added_by"), rs.getString("text")), sheetId);
    }

    public void save(Note note) {
        jdbc.update("INSERT INTO sheet_note (sheet_id, added_on, added_by, text) VALUES (?, ?, ?, ?)", note.sheetId(),
                Date.valueOf(note.addedOn()), note.addedBy(), note.text());
    }
}
