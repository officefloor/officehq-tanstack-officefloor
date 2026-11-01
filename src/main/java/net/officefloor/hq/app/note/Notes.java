package net.officefloor.hq.app.note;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The notes of each sheet. */
@Repository
public class Notes {

    private final JdbcTemplate jdbc;

    public Notes(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Every note on the sheet, newest first. */
    public List<Note> onSheet(String sheetId) {
        return jdbc.query("SELECT sheet_id, text, by_user, added_on FROM sheet_note WHERE sheet_id = ? ORDER BY id DESC",
                (rs, i) -> new Note(rs.getString(1), rs.getString(2), rs.getString(3),
                        rs.getDate(4).toLocalDate()), sheetId);
    }

    public void add(Note note) {
        jdbc.update("INSERT INTO sheet_note (sheet_id, text, by_user, added_on) VALUES (?, ?, ?, ?)",
                note.sheetId(), note.text(), note.by(), java.sql.Date.valueOf(note.addedOn()));
    }
}
