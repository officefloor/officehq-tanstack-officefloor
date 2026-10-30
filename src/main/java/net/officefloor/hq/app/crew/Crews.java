package net.officefloor.hq.app.crew;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The crew store: each sheet's crews, in the order added, with their leads and members. */
@Repository
public class Crews {

    private final JdbcTemplate jdbc;

    public Crews(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's crews, in the order added, each with its members in the order added. */
    public List<CrewResponse> list(String sheetId) {
        Map<String, String[]> leads = new LinkedHashMap<>();
        Map<String, List<CrewResponse.Member>> members = new LinkedHashMap<>();
        jdbc.query("SELECT c.name, c.lead_person_id, p.name FROM crew c"
                + " LEFT JOIN person p ON p.id = c.lead_person_id AND p.sheet_id = c.sheet_id"
                + " WHERE c.sheet_id = ? ORDER BY c.seq", rs -> {
                    leads.put(rs.getString(1), new String[] { String.valueOf(rs.getLong(2)), rs.getString(3) });
                    members.put(rs.getString(1), new ArrayList<>());
                }, sheetId);
        jdbc.query("SELECT m.crew_name, m.person_id, p.name FROM crew_member m"
                + " JOIN person p ON p.id = m.person_id AND p.sheet_id = m.sheet_id"
                + " WHERE m.sheet_id = ? ORDER BY m.seq", rs -> {
                    List<CrewResponse.Member> list = members.get(rs.getString(1));
                    if (list != null) {
                        list.add(new CrewResponse.Member(String.valueOf(rs.getLong(2)), rs.getString(3)));
                    }
                }, sheetId);
        List<CrewResponse> crews = new ArrayList<>();
        leads.forEach((name, lead) -> crews.add(new CrewResponse(name, lead[0], lead[1] == null ? "" : lead[1], members.get(name))));
        return crews;
    }

    /** Whether the sheet has a crew of the name. */
    public boolean contains(String sheetId, String name) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM crew WHERE sheet_id = ? AND name = ?", Integer.class, sheetId, name);
        return n != null && n > 0;
    }

    /** The id of the crew's lead. */
    public long leadOf(String sheetId, String name) {
        return jdbc.queryForObject("SELECT lead_person_id FROM crew WHERE sheet_id = ? AND name = ?", Long.class, sheetId, name);
    }

    /** Whether the person is a member of the crew. */
    public boolean hasMember(String sheetId, String crew, long personId) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM crew_member WHERE sheet_id = ? AND crew_name = ? AND person_id = ?",
                Integer.class, sheetId, crew, personId);
        return n != null && n > 0;
    }

    /** Adds a crew with its lead to the sheet. */
    public void add(String sheetId, String name, long leadId) {
        jdbc.update("INSERT INTO crew (sheet_id, name, lead_person_id) VALUES (?, ?, ?)", sheetId, name, leadId);
    }

    /** Adds the person to the crew. */
    public void addMember(String sheetId, String crew, long personId) {
        jdbc.update("INSERT INTO crew_member (sheet_id, crew_name, person_id) VALUES (?, ?, ?)", sheetId, crew, personId);
    }
}
