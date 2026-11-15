package net.officefloor.hq.app.note;

import java.time.LocalDate;
import java.util.List;
import net.officefloor.hq.app.asof.AsOfView;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class NoteRepository {

    private static final RowMapper<Note> ROW = (rs, n) -> new Note(rs.getLong("id"), rs.getString("sheet_id"),
            rs.getString("text"), rs.getString("added_by"), rs.getObject("added_on", LocalDate.class).toString());

    private final JdbcTemplate jdbc;

    public NoteRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's notes, newest first. Shown as of a date (the view's today), only those added while the
     *  sheet's today was on or before it. */
    public List<Note> list(String sheetId) {
        String sql = "SELECT id, sheet_id, text, added_by, added_on FROM sheet_note WHERE sheet_id = ?";
        if (AsOfView.schema() != null) {
            sql += " AND added_on <= (SELECT today FROM sheet WHERE id = sheet_note.sheet_id)";
        }
        return jdbc.query(sql + " ORDER BY id DESC", ROW, sheetId);
    }

    public Note add(String sheetId, String text, String by, LocalDate today) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(c -> {
            var ps = c.prepareStatement(
                    "INSERT INTO sheet_note (sheet_id, text, added_by, added_on) VALUES (?, ?, ?, ?)",
                    new String[] { "id" });
            ps.setString(1, sheetId);
            ps.setString(2, text);
            ps.setString(3, by);
            ps.setObject(4, today);
            return ps;
        }, keys);
        return new Note(keys.getKey().longValue(), sheetId, text, by, today.toString());
    }
}
