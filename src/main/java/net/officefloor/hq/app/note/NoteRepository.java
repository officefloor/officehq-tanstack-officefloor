package net.officefloor.hq.app.note;

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

    /** The sheet's notes added while its today was on or before the date, newest first. */
    public List<Note> findUpTo(String sheetId, LocalDate upTo) {
        return jdbc.query("SELECT sheet_id, note_date, by_user, text FROM sheet_note WHERE sheet_id = ? AND note_date <= ?"
                + " ORDER BY id DESC",
                (rs, i) -> new Note(rs.getString("sheet_id"), rs.getObject("note_date", LocalDate.class),
                        rs.getString("by_user"), rs.getString("text")),
                sheetId, upTo);
    }

    /** The sheet's notes, newest first. */
    public List<Note> findAll(String sheetId) {
        return jdbc.query("SELECT sheet_id, note_date, by_user, text FROM sheet_note WHERE sheet_id = ? ORDER BY id DESC",
                (rs, i) -> new Note(rs.getString("sheet_id"), rs.getObject("note_date", LocalDate.class),
                        rs.getString("by_user"), rs.getString("text")),
                sheetId);
    }

    public void add(Note note) {
        jdbc.update("INSERT INTO sheet_note (sheet_id, by_user, note_date, text) VALUES (?, ?, ?, ?)", note.sheetId(),
                note.user(), note.date(), note.text());
    }
}
