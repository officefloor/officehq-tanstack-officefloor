package net.officefloor.hq.app.crew;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The crews of each sheet, in the order added, and each crew's members in the order added. */
@Repository
public class Crews {

    private final JdbcTemplate jdbc;

    public Crews(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Every crew on the sheet with its lead and its members (a person no longer on the sheet left out). */
    public List<CrewResponse> onSheet(String sheetId) {
        Map<String, Object[]> crews = new LinkedHashMap<>();
        Map<String, List<CrewMemberResponse>> members = new LinkedHashMap<>();
        jdbc.query("SELECT c.name, c.lead_id, p.name FROM crew c LEFT JOIN person p ON p.id = c.lead_id "
                + "WHERE c.sheet_id = ? ORDER BY c.position", rs -> {
                    crews.put(rs.getString(1), new Object[] { rs.getLong(2), rs.getString(3) });
                    members.put(rs.getString(1), new ArrayList<>());
                }, sheetId);
        jdbc.query("SELECT m.crew, m.person_id, p.name FROM crew_member m JOIN person p ON p.id = m.person_id "
                + "WHERE m.sheet_id = ? ORDER BY m.position", rs -> {
                    List<CrewMemberResponse> list = members.get(rs.getString(1));
                    if (list != null) list.add(new CrewMemberResponse(rs.getLong(2), rs.getString(3)));
                }, sheetId);
        List<CrewResponse> out = new ArrayList<>();
        crews.forEach((name, lead) -> out.add(new CrewResponse(name, (Long) lead[0], (String) lead[1],
                members.get(name))));
        return out;
    }

    /** Whether the sheet has a crew of that name. */
    public boolean has(String sheetId, String name) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM crew WHERE sheet_id = ? AND name = ?",
                Integer.class, sheetId, name);
        return n != null && n > 0;
    }

    /** Whether the sheet has a pool of that name. */
    public boolean hasPool(String sheetId, String name) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM pool WHERE sheet_id = ? AND name = ?",
                Integer.class, sheetId, name);
        return n != null && n > 0;
    }

    /** The lead of the crew, or null when the sheet has no such crew. */
    public Long leadOf(String sheetId, String crew) {
        List<Long> leads = jdbc.query("SELECT lead_id FROM crew WHERE sheet_id = ? AND name = ?",
                (rs, i) -> rs.getLong(1), sheetId, crew);
        return leads.isEmpty() ? null : leads.get(0);
    }

    /** Whether the person is a member of the crew. */
    public boolean hasMember(String sheetId, String crew, long personId) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM crew_member WHERE sheet_id = ? AND crew = ? AND person_id = ?",
                Integer.class, sheetId, crew, personId);
        return n != null && n > 0;
    }

    public void add(String sheetId, String name, long leadId) {
        jdbc.update("INSERT INTO crew (sheet_id, name, lead_id, position) VALUES (?, ?, ?, "
                + "(SELECT COALESCE(MAX(position), 0) + 1 FROM crew WHERE sheet_id = ?))",
                sheetId, name, leadId, sheetId);
    }

    public void addMember(String sheetId, String crew, long personId) {
        jdbc.update("INSERT INTO crew_member (sheet_id, crew, person_id, position) VALUES (?, ?, ?, "
                + "(SELECT COALESCE(MAX(position), 0) + 1 FROM crew_member WHERE sheet_id = ?))",
                sheetId, crew, personId, sheetId);
    }

    /** The name of the sheet's person with that id, or null when the sheet has none. */
    public String personName(String sheetId, long personId) {
        List<String> names = jdbc.query("SELECT name FROM person WHERE sheet_id = ? AND id = ?",
                (rs, i) -> rs.getString(1), sheetId, personId);
        return names.isEmpty() ? null : names.get(0);
    }
}
