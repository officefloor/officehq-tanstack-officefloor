package net.officefloor.hq.app.crew;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class CrewRepository {

    private final JdbcTemplate jdbc;

    public CrewRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's crews in the order they were added, each with its members in the order they were added. */
    public List<Crew> list(String sheetId) {
        Map<Long, Object[]> crews = new LinkedHashMap<>();
        Map<Long, List<CrewMember>> members = new LinkedHashMap<>();
        jdbc.query("SELECT c.id, c.name, c.lead_id, p.name AS lead FROM crew c LEFT JOIN person p ON p.id = c.lead_id "
                + "WHERE c.sheet_id = ? ORDER BY c.id", rs -> {
                    crews.put(rs.getLong("id"), new Object[] { rs.getString("name"), rs.getObject("lead_id", Long.class), rs.getString("lead") == null ? "" : rs.getString("lead") });
                    members.put(rs.getLong("id"), new ArrayList<>());
                }, sheetId);
        jdbc.query("SELECT m.crew_id, m.person_id, p.name FROM crew_member m JOIN person p ON p.id = m.person_id "
                + "WHERE m.sheet_id = ? ORDER BY m.id", rs -> {
                    List<CrewMember> of = members.get(rs.getLong("crew_id"));
                    if (of != null) of.add(new CrewMember(rs.getLong("person_id"), rs.getString("name")));
                }, sheetId);
        List<Crew> list = new ArrayList<>();
        for (Map.Entry<Long, Object[]> crew : crews.entrySet()) {
            Object[] c = crew.getValue();
            list.add(new Crew(crew.getKey(), sheetId, (String) c[0], (Long) c[1], (String) c[2], members.get(crew.getKey())));
        }
        return list;
    }

    /** The crew, or null when there is no such crew on the sheet. */
    public Crew find(String sheetId, long crewId) {
        return list(sheetId).stream().filter(c -> c.id() == crewId).findFirst().orElse(null);
    }

    public boolean contains(String sheetId, String name) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM crew WHERE sheet_id = ? AND name = ?",
                Integer.class, sheetId, name);
        return n != null && n > 0;
    }

    public void add(String sheetId, String name, long leadId, String by) {
        jdbc.update("INSERT INTO crew (sheet_id, name, lead_id, added_by) VALUES (?, ?, ?, ?)", sheetId, name, leadId, by);
    }

    public void addMember(String sheetId, long crewId, long personId, String by) {
        jdbc.update("INSERT INTO crew_member (sheet_id, crew_id, person_id, added_by) VALUES (?, ?, ?, ?)",
                sheetId, crewId, personId, by);
    }
}
