package net.officefloor.hq.app.crew;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.officefloor.hq.app.history.History;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The crews per sheet: a name, a lead, and their members, people on the sheet. */
@Repository
public class CrewStore {

    /** A crew as stored: its lead's id and name (null when the lead is no longer on the sheet). */
    public record Crew(long id, String name, long leadId, String leadName) {
    }

    /** A member of a crew: the person and their name. */
    public record Member(long crewId, long personId, String name) {
    }

    private final JdbcTemplate jdbc;

    public CrewStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's crews, in the order they were added; as of the date when one is given (see {@link History}). */
    public List<Crew> list(String sheetId, LocalDate on) {
        String crew = on == null ? "crew" : History.asOf("crew", "id", on);
        String person = on == null ? "person" : History.asOf("person", "id", on);
        return jdbc.query("SELECT crew.id, crew.name, crew.lead_id, person.name AS lead_name FROM " + crew
                + " LEFT JOIN " + person + " ON person.id = crew.lead_id WHERE crew.sheet_id = ? ORDER BY crew.id",
                (rs, i) -> new Crew(rs.getLong("id"), rs.getString("name"), rs.getLong("lead_id"),
                        rs.getString("lead_name")), sheetId);
    }

    /** The members of the sheet's crews still on the sheet, by crew id, each crew's in the order they were added. */
    public Map<Long, List<Member>> members(String sheetId, LocalDate on) {
        String member = on == null ? "crew_member" : History.asOf("crew_member", "id", on);
        String person = on == null ? "person" : History.asOf("person", "id", on);
        Map<Long, List<Member>> found = new LinkedHashMap<>();
        jdbc.query("SELECT crew_member.crew_id, crew_member.person_id, person.name FROM " + member
                + " JOIN " + person + " ON person.id = crew_member.person_id"
                + " WHERE crew_member.sheet_id = ? ORDER BY crew_member.id", rs -> {
                    Member m = new Member(rs.getLong("crew_id"), rs.getLong("person_id"), rs.getString("name"));
                    found.computeIfAbsent(m.crewId(), k -> new ArrayList<>()).add(m);
                }, sheetId);
        return found;
    }

    public Crew find(String sheetId, long id) {
        List<Crew> found = jdbc.query("SELECT id, name, lead_id FROM crew WHERE sheet_id = ? AND id = ?",
                (rs, i) -> new Crew(rs.getLong("id"), rs.getString("name"), rs.getLong("lead_id"), null), sheetId, id);
        return found.isEmpty() ? null : found.get(0);
    }

    public boolean has(String sheetId, String name) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM crew WHERE sheet_id = ? AND name = ?", Integer.class,
                sheetId, name);
        return n != null && n > 0;
    }

    public void add(String sheetId, String name, long leadId) {
        jdbc.update("INSERT INTO crew (sheet_id, name, lead_id) VALUES (?, ?, ?)", sheetId, name, leadId);
    }

    public boolean isMember(long crewId, long personId) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM crew_member WHERE crew_id = ? AND person_id = ?",
                Integer.class, crewId, personId);
        return n != null && n > 0;
    }

    public void addMember(String sheetId, long crewId, long personId) {
        jdbc.update("INSERT INTO crew_member (sheet_id, crew_id, person_id) VALUES (?, ?, ?)", sheetId, crewId,
                personId);
    }
}
