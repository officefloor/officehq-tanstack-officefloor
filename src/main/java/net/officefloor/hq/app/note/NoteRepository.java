package net.officefloor.hq.app.note;

import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

/** The notes by sheet. */
@Repository
public class NoteRepository {

    private static final RowMapper<Note> ROW = (rs, i) -> new Note(rs.getLong("id"), rs.getString("sheet_id"),
            rs.getString("text"), rs.getString("added_by"), rs.getObject("added_on", LocalDate.class));

    private final JdbcTemplate jdbc;

    public NoteRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's notes, newest first. */
    public List<Note> list(String sheetId) {
        return jdbc.query("SELECT * FROM sheet_note WHERE sheet_id = ? ORDER BY id DESC", ROW, sheetId);
    }

    public Note add(String sheetId, NoteInput input, LocalDate on) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(con -> {
            var ps = con.prepareStatement("INSERT INTO sheet_note (sheet_id, text, added_by, added_on) VALUES (?, ?, ?, ?)",
                    new String[] { "id" });
            ps.setString(1, sheetId);
            ps.setString(2, input.text());
            ps.setString(3, input.user());
            ps.setObject(4, on);
            return ps;
        }, keys);
        return new Note(keys.getKey().longValue(), sheetId, input.text(), input.user(), on);
    }
}
