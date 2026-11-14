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

    /** The sheet's crews with their leads (null: none) and members, each in the order they were added. */
    public List<Crew> findAll(String sheetId) {
        Map<Long, Crew> crews = new LinkedHashMap<>();
        jdbc.query("SELECT c.id, c.name, c.lead_id, p.name AS lead_name FROM crew c LEFT JOIN person p ON p.id = c.lead_id"
                + " WHERE c.sheet_id = ? ORDER BY c.id", rs -> {
                    long id = rs.getLong("id");
                    long leadId = rs.getLong("lead_id");
                    CrewPerson lead = rs.wasNull() ? null : new CrewPerson(leadId, rs.getString("lead_name"));
                    crews.put(id, new Crew(id, sheetId, rs.getString("name"), lead, new ArrayList<>()));
                }, sheetId);
        jdbc.query("SELECT m.crew_id, m.person_id, p.name FROM crew_member m"
                + " JOIN person p ON p.id = m.person_id WHERE m.sheet_id = ? ORDER BY m.id",
                rs -> {
                    crews.get(rs.getLong("crew_id")).members()
                            .add(new CrewPerson(rs.getLong("person_id"), rs.getString("name")));
                }, sheetId);
        return new ArrayList<>(crews.values());
    }

    /** The sheet's crew with this id, or null. */
    public Crew find(String sheetId, long id) {
        return findAll(sheetId).stream().filter(c -> c.id() == id).findFirst().orElse(null);
    }

    /** Whether the sheet has a crew of this name. */
    public boolean nameTaken(String sheetId, String name) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM crew WHERE sheet_id = ? AND name = ?", Integer.class,
                sheetId, name);
        return n != null && n > 0;
    }

    /** Adds a crew with its lead and no members. */
    public void add(Crew crew) {
        jdbc.update("INSERT INTO crew (sheet_id, name, lead_id) VALUES (?, ?, ?)", crew.sheetId(), crew.name(),
                crew.lead().id());
    }

    /** Adds a member to the crew. */
    public void addMember(Crew crew, CrewPerson member) {
        jdbc.update("INSERT INTO crew_member (sheet_id, crew_id, person_id) VALUES (?, ?, ?)", crew.sheetId(),
                crew.id(), member.id());
    }
}
