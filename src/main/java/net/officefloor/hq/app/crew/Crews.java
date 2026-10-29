package net.officefloor.hq.app.crew;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.officefloor.hq.app.history.AsOf;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Repository;

/** The store of the sheet's crews and their members. */
@Repository
public class Crews {

    private final JdbcTemplate jdbc;

    public Crews(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's crews, in the order they were added, each with its members' ids in the order they were added. */
    public List<CrewResponse> list(String sheetId) {
        Map<String, Long> leads = new LinkedHashMap<>();
        jdbc.query("SELECT name, lead_id FROM " + AsOf.table("crew") + " WHERE sheet_id = ? ORDER BY listed",
                (RowCallbackHandler) rs -> leads.put(rs.getString("name"), rs.getLong("lead_id")), sheetId);
        Map<String, List<Long>> members = new LinkedHashMap<>();
        jdbc.query("SELECT crew_name, person_id FROM " + AsOf.table("crew_member") + " WHERE sheet_id = ? ORDER BY listed",
                (RowCallbackHandler) rs -> members.computeIfAbsent(rs.getString("crew_name"), k -> new ArrayList<>())
                        .add(rs.getLong("person_id")),
                sheetId);
        List<CrewResponse> out = new ArrayList<>();
        leads.forEach((name, lead) -> out.add(new CrewResponse(name, lead, members.getOrDefault(name, List.of()))));
        return out;
    }

    /** The crew's lead's id, null when the sheet has no such crew. */
    public Long lead(String sheetId, String name) {
        List<Long> found = jdbc.queryForList("SELECT lead_id FROM crew WHERE sheet_id = ? AND name = ?", Long.class,
                sheetId, name);
        return found.isEmpty() ? null : found.get(0);
    }

    public boolean has(String sheetId, String name) {
        return lead(sheetId, name) != null;
    }

    public void add(Crew crew) {
        jdbc.update("INSERT INTO crew (sheet_id, name, lead_id, listed) VALUES (?, ?, ?, "
                + "(SELECT COALESCE(MAX(listed), 0) + 1 FROM crew WHERE sheet_id = ?))",
                crew.sheetId(), crew.name(), crew.leadId(), crew.sheetId());
    }

    public boolean isMember(String sheetId, String crew, long personId) {
        Integer n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM crew_member WHERE sheet_id = ? AND crew_name = ? AND person_id = ?", Integer.class,
                sheetId, crew, personId);
        return n != null && n > 0;
    }

    public void addMember(CrewMember member) {
        jdbc.update("INSERT INTO crew_member (sheet_id, crew_name, person_id, listed) VALUES (?, ?, ?, "
                + "(SELECT COALESCE(MAX(listed), 0) + 1 FROM crew_member WHERE sheet_id = ?))",
                member.sheetId(), member.crew(), member.personId(), member.sheetId());
    }
}
