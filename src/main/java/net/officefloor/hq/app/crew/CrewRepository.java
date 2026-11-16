package net.officefloor.hq.app.crew;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The stored crews and their members, kept per sheet. */
@Repository
public class CrewRepository {

    private final JdbcTemplate jdbc;

    public CrewRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's crews in the order added, each with its lead and its members in the order added. */
    public List<Crew> list(String sheetId) {
        Map<Long, Crew> crews = new LinkedHashMap<>();
        jdbc.query("SELECT c.id, c.name, c.lead_id, p.name AS lead FROM crew c JOIN person p ON p.id = c.lead_id"
                + " WHERE c.sheet_id = ? ORDER BY c.id",
                rs -> {
                    crews.put(rs.getLong("id"), new Crew(rs.getString("name"), rs.getLong("lead_id"),
                            rs.getString("lead"), new ArrayList<>()));
                }, sheetId);
        jdbc.query("SELECT m.crew_id, m.person_id, p.name FROM crew_member m JOIN crew c ON c.id = m.crew_id"
                + " JOIN person p ON p.id = m.person_id WHERE c.sheet_id = ? ORDER BY m.id",
                rs -> {
                    Crew crew = crews.get(rs.getLong("crew_id"));
                    if (crew != null) {
                        crew.members().add(new CrewMember(rs.getLong("person_id"), rs.getString("name")));
                    }
                }, sheetId);
        return new ArrayList<>(crews.values());
    }

    /** Adds the crew; false when the name is already taken on the sheet (the unique key refuses a second). */
    public boolean add(String sheetId, String name, long leadId) {
        try {
            jdbc.update("INSERT INTO crew (sheet_id, name, lead_id) VALUES (?, ?, ?)", sheetId, name, leadId);
            return true;
        } catch (DuplicateKeyException e) {
            return false;
        }
    }

    /** The id of the sheet's crew by that name, or null when there is none. */
    public Long idOf(String sheetId, String name) {
        List<Long> ids = jdbc.queryForList("SELECT id FROM crew WHERE sheet_id = ? AND name = ?", Long.class, sheetId,
                name);
        return ids.isEmpty() ? null : ids.get(0);
    }

    /** The id of the crew's lead. */
    public long leadOf(long crewId) {
        return jdbc.queryForObject("SELECT lead_id FROM crew WHERE id = ?", Long.class, crewId);
    }

    /** The name of the person on the sheet, or null when there is no such person there. */
    public String personName(String sheetId, long personId) {
        List<String> names = jdbc.queryForList("SELECT name FROM person WHERE sheet_id = ? AND id = ?", String.class,
                sheetId, personId);
        return names.isEmpty() ? null : names.get(0);
    }

    /** Adds the person to the crew; false when they are already in it. */
    public boolean addMember(long crewId, long personId) {
        try {
            jdbc.update("INSERT INTO crew_member (crew_id, person_id) VALUES (?, ?)", crewId, personId);
            return true;
        } catch (DuplicateKeyException e) {
            return false;
        }
    }
}
