package net.officefloor.hq.app.crew;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/** The crews by sheet, and their members. */
@Repository
public class CrewRepository {

    private static final RowMapper<Crew> ROW = (rs, i) -> new Crew(rs.getLong("id"), rs.getString("sheet_id"),
            rs.getString("name"), rs.getLong("lead_id"), rs.getString("added_by"));

    private final JdbcTemplate jdbc;

    public CrewRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's crews in the order they were added. */
    public List<Crew> list(String sheetId) {
        return jdbc.query("SELECT * FROM crew WHERE sheet_id = ? ORDER BY id", ROW, sheetId);
    }

    /** The crew on this sheet with this name, or null when absent. */
    public Crew find(String sheetId, String name) {
        return jdbc.query("SELECT * FROM crew WHERE sheet_id = ? AND name = ?", ROW, sheetId, name).stream()
                .findFirst().orElse(null);
    }

    /** The crew on this sheet with this id, or null when absent. */
    public Crew find(String sheetId, long id) {
        return jdbc.query("SELECT * FROM crew WHERE sheet_id = ? AND id = ?", ROW, sheetId, id).stream()
                .findFirst().orElse(null);
    }

    public Crew add(String sheetId, CrewInput input) {
        jdbc.update("INSERT INTO crew (sheet_id, name, lead_id, added_by) VALUES (?, ?, ?, ?)", sheetId,
                input.name(), input.leadId(), input.user());
        return find(sheetId, input.name());
    }

    public boolean isMember(Crew crew, long personId) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM crew_member WHERE crew_id = ? AND person_id = ?",
                Integer.class, crew.id(), personId);
        return n != null && n > 0;
    }

    public void addMember(Crew crew, long personId, String user) {
        jdbc.update("INSERT INTO crew_member (sheet_id, crew_id, person_id, added_by) VALUES (?, ?, ?, ?)",
                crew.sheetId(), crew.id(), personId, user);
    }

    /** The person ids of the crew's members in the order they were added. */
    public List<Long> members(Crew crew) {
        return jdbc.queryForList("SELECT person_id FROM crew_member WHERE crew_id = ? ORDER BY id", Long.class,
                crew.id());
    }
}
