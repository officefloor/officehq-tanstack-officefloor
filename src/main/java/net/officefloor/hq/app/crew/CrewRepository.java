package net.officefloor.hq.app.crew;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class CrewRepository {

    private final JdbcTemplate jdbc;

    public CrewRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public boolean sheetExists(String sheetId) {
        Integer known = jdbc.queryForObject("SELECT COUNT(*) FROM sheet WHERE id = ?", Integer.class, sheetId);
        return known != null && known > 0;
    }

    /** The person's name, or null when they are not on the sheet. */
    public String personName(String sheetId, long personId) {
        List<String> names = jdbc.queryForList("SELECT name FROM person WHERE sheet_id = ? AND id = ?",
                String.class, sheetId, personId);
        return names.isEmpty() ? null : names.get(0);
    }

    /** Whether the sheet has a pool of that name (a crew is also a pool, so may not share a pool's name). */
    public boolean poolExists(String sheetId, String name) {
        Integer known = jdbc.queryForObject("SELECT COUNT(*) FROM pool WHERE sheet_id = ? AND name = ?",
                Integer.class, sheetId, name);
        return known != null && known > 0;
    }

    /** The crew's lead (by id), or null when the sheet has no crew of that name. */
    public Long leadOf(String sheetId, String crew) {
        List<Long> found = jdbc.queryForList("SELECT lead_id FROM crew WHERE sheet_id = ? AND name = ?", Long.class,
                sheetId, crew);
        return found.isEmpty() ? null : found.get(0);
    }

    /** Add a crew; false when the sheet already has one of that name. */
    public boolean add(String sheetId, String name, long leadId) {
        try {
            jdbc.update("INSERT INTO crew (sheet_id, name, lead_id) VALUES (?, ?, ?)", sheetId, name, leadId);
            return true;
        } catch (DuplicateKeyException e) {
            return false;
        }
    }

    /** Add a member to a crew; false when the person is already in it. */
    public boolean addMember(String sheetId, String crew, long personId) {
        try {
            jdbc.update("INSERT INTO crew_member (sheet_id, crew_name, person_id) VALUES (?, ?, ?)", sheetId, crew,
                    personId);
            return true;
        } catch (DuplicateKeyException e) {
            return false;
        }
    }

    /** The sheet's crews, by name, each with its lead and its members (people still on the sheet) in the order they were added. */
    public List<Crew> list(String sheetId) {
        Map<String, Crew> crews = new LinkedHashMap<>();
        jdbc.query("SELECT c.name, c.lead_id, p.name AS lead FROM crew c LEFT JOIN person p ON p.id = c.lead_id"
                + " WHERE c.sheet_id = ? ORDER BY c.name", rs -> {
            crews.put(rs.getString("name"), new Crew(rs.getString("name"), rs.getLong("lead_id"),
                    rs.getString("lead") == null ? "" : rs.getString("lead"), new ArrayList<>()));
        }, sheetId);
        jdbc.query("SELECT m.crew_name, m.person_id, p.name FROM crew_member m JOIN person p ON p.id = m.person_id"
                + " WHERE m.sheet_id = ? ORDER BY m.id", rs -> {
            Crew crew = crews.get(rs.getString("crew_name"));
            if (crew != null) crew.members().add(new CrewMember(rs.getLong("person_id"), rs.getString("name")));
        }, sheetId);
        return new ArrayList<>(crews.values());
    }
}
