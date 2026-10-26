package net.officefloor.hq.app.crew;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The crews of each sheet, in the order added, each name at most once on its sheet, with a lead and members. */
@Repository
public class CrewRepository {

    private final JdbcTemplate jdbc;

    public CrewRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The id of the sheet's crew of that name; null when there is none. */
    public Long findId(String sheetId, String name) {
        return jdbc.query("SELECT id FROM crew WHERE sheet_id = ? AND name = ?", (rs, n) -> rs.getLong(1), sheetId, name)
                .stream().findFirst().orElse(null);
    }

    public void insert(String sheetId, String name, long leadId) {
        jdbc.update("INSERT INTO crew (sheet_id, name, lead_id) VALUES (?, ?, ?)", sheetId, name, leadId);
    }

    /** The id of the crew's lead. */
    public long leadOf(long crewId) {
        return jdbc.queryForObject("SELECT lead_id FROM crew WHERE id = ?", Long.class, crewId);
    }

    /** Adds the person to the crew; false when they are in it already. */
    public boolean addMember(String sheetId, long crewId, long personId) {
        return jdbc.update("INSERT INTO crew_member (sheet_id, crew_id, person_id)"
                + " SELECT ?, ?, ? WHERE NOT EXISTS (SELECT 1 FROM crew_member WHERE crew_id = ? AND person_id = ?)",
                sheetId, crewId, personId, crewId, personId) > 0;
    }

    /** The sheet's crews, in the order added, each with its lead and its members in the order added. */
    public List<CrewResponse> list(String sheetId) {
        Map<Long, String> names = new LinkedHashMap<>();
        Map<Long, CrewResponse.Person> leads = new LinkedHashMap<>();
        Map<Long, List<CrewResponse.Person>> members = new LinkedHashMap<>();
        jdbc.query("SELECT c.id, c.name, c.lead_id, p.name AS lead_name FROM crew c JOIN person p ON p.id = c.lead_id"
                + " WHERE c.sheet_id = ? ORDER BY c.id", rs -> {
            long id = rs.getLong("id");
            names.put(id, rs.getString("name"));
            leads.put(id, new CrewResponse.Person(rs.getLong("lead_id"), rs.getString("lead_name")));
            members.put(id, new ArrayList<>());
        }, sheetId);
        jdbc.query("SELECT m.crew_id, m.person_id, p.name FROM crew_member m JOIN person p ON p.id = m.person_id"
                + " WHERE m.sheet_id = ? ORDER BY m.id", rs -> {
            List<CrewResponse.Person> of = members.get(rs.getLong("crew_id"));
            if (of != null) {
                of.add(new CrewResponse.Person(rs.getLong("person_id"), rs.getString("name")));
            }
        }, sheetId);
        List<CrewResponse> crews = new ArrayList<>();
        names.forEach((id, name) -> crews.add(new CrewResponse(name, leads.get(id), members.get(id))));
        return crews;
    }
}
