package net.officefloor.hq.app.crew;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The crews on each sheet, their leads and their members. */
@Repository
public class Crews {

    private final JdbcTemplate jdbc;

    public Crews(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Every crew on the sheet in the order added, with its lead and its members in the order added. */
    public List<Crew> list(String sheet) {
        Map<String, CrewMember> leads = new LinkedHashMap<>();
        jdbc.query("SELECT c.name, c.lead_id, p.name AS lead_name FROM crew c"
                + " LEFT JOIN person p ON p.sheet = c.sheet AND p.id = c.lead_id WHERE c.sheet = ? ORDER BY c.position",
                (rs) -> {
                    // A crew whose lead has gone from the sheet (e.g. transferred away) is still listed, with no lead.
                    String lead = rs.getString("lead_name");
                    leads.put(rs.getString("name"),
                            lead == null ? new CrewMember(0, "") : new CrewMember(rs.getLong("lead_id"), lead));
                }, sheet);
        Map<String, List<CrewMember>> members = new LinkedHashMap<>();
        leads.keySet().forEach((name) -> members.put(name, new ArrayList<>()));
        jdbc.query("SELECT m.crew, m.person_id, p.name FROM crew_member m"
                + " JOIN person p ON p.sheet = m.sheet AND p.id = m.person_id WHERE m.sheet = ? ORDER BY m.position",
                (rs) -> {
                    List<CrewMember> list = members.get(rs.getString("crew"));
                    if (list != null) {
                        list.add(new CrewMember(rs.getLong("person_id"), rs.getString("name")));
                    }
                }, sheet);
        return leads.entrySet().stream()
                .map((e) -> new Crew(e.getKey(), e.getValue(), List.copyOf(members.get(e.getKey()))))
                .toList();
    }

    /** Whether the sheet has a crew of this name. */
    public boolean has(String sheet, String name) {
        return leadOf(sheet, name) != null;
    }

    /** The id of the crew's lead, or null when the sheet has no crew of this name. */
    public Long leadOf(String sheet, String name) {
        List<Long> found = jdbc.queryForList("SELECT lead_id FROM crew WHERE sheet = ? AND name = ?", Long.class,
                sheet, name);
        return found.isEmpty() ? null : found.get(0);
    }

    /** Whether the person is in the crew. */
    public boolean isMember(String sheet, String crew, long person) {
        Integer found = jdbc.queryForObject(
                "SELECT COUNT(*) FROM crew_member WHERE sheet = ? AND crew = ? AND person_id = ?", Integer.class, sheet,
                crew, person);
        return found != null && found > 0;
    }

    public void add(String sheet, String name, long lead) {
        jdbc.update("INSERT INTO crew (sheet, name, lead_id, position) VALUES (?, ?, ?,"
                + " (SELECT COALESCE(MAX(c.position), 0) + 1 FROM crew c WHERE c.sheet = ?))", sheet, name, lead, sheet);
    }

    public void addMember(String sheet, String crew, long person) {
        jdbc.update("INSERT INTO crew_member (sheet, crew, person_id, position) VALUES (?, ?, ?,"
                + " (SELECT COALESCE(MAX(m.position), 0) + 1 FROM crew_member m WHERE m.sheet = ?))", sheet, crew,
                person, sheet);
    }
}
