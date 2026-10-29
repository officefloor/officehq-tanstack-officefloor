package net.officefloor.hq.app.note;

import java.time.LocalDate;
import java.util.List;
import net.officefloor.hq.app.history.AsOf;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The store of the sheet's notes. */
@Repository
public class Notes {

    private final JdbcTemplate jdbc;

    public Notes(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's notes, newest first; read as of a date, only those added while the sheet's today was on or before it. */
    public List<NoteResponse> list(String sheetId) {
        LocalDate asOf = AsOf.date();
        return jdbc.query("SELECT id, added_on, added_by, text FROM sheet_note WHERE sheet_id = ?"
                + (asOf == null ? "" : " AND added_on <= DATE '" + asOf + "'") + " ORDER BY id DESC",
                (rs, n) -> new NoteResponse(rs.getLong("id"), rs.getDate("added_on").toLocalDate().toString(),
                        rs.getString("added_by"), rs.getString("text")),
                sheetId);
    }

    public void add(Note note) {
        jdbc.update("INSERT INTO sheet_note (sheet_id, added_on, added_by, text) VALUES (?, ?, ?, ?)", note.sheetId(),
                note.on(), note.by(), note.text());
    }
}
