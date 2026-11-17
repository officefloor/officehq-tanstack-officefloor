package net.officefloor.hq.app.crew;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.officefloor.hq.app.sheet.SheetRepository;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The stored crews and their members, kept per sheet. */
@Repository
public class CrewRepository {

    private final JdbcTemplate jdbc;

    private final SheetRepository sheets;

    public CrewRepository(JdbcTemplate jdbc, SheetRepository sheets) {
        this.jdbc = jdbc;
        this.sheets = sheets;
    }

    /** The sheet's crews in the order added, each with its lead and its members in the order added. */
    public List<Crew> list(String sheetId) {
        Map<Long, Crew> crews = new LinkedHashMap<>();
        jdbc.query("SELECT c.id, c.name, c.lead_id, p.name AS lead FROM crew c LEFT JOIN person p ON p.id = c.lead_id"
                + " WHERE c.sheet_id = ? ORDER BY c.id",
                rs -> {
                    long leadId = rs.getLong("lead_id");
                    Long lead = rs.wasNull() ? null : leadId;
                    crews.put(rs.getLong("id"), new Crew(rs.getString("name"), lead,
                            lead == null ? "" : rs.getString("lead"), new ArrayList<>()));
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
            jdbc.update("INSERT INTO crew (sheet_id, name, lead_id, made_on) VALUES (?, ?, ?, ?)", sheetId, name, leadId,
                    java.sql.Date.valueOf(sheets.today(sheetId)));
            return true;
        } catch (DuplicateKeyException e) {
            return false;
        }
    }

    /** Whether the sheet has a pool by that name (a crew cannot take a pool's name). */
    public boolean isPool(String sheetId, String name) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM pool WHERE sheet_id = ? AND name = ?", Integer.class, sheetId,
                name) > 0;
    }

    /** The id of the sheet's crew by that name, or null when there is none. */
    public Long idOf(String sheetId, String name) {
        List<Long> ids = jdbc.queryForList("SELECT id FROM crew WHERE sheet_id = ? AND name = ?", Long.class, sheetId,
                name);
        return ids.isEmpty() ? null : ids.get(0);
    }

    /** The id of the crew's lead, or null when it has none (its lead has left the sheet). */
    public Long leadOf(long crewId) {
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
            String sheetId = jdbc.queryForObject("SELECT sheet_id FROM crew WHERE id = ?", String.class, crewId);
            jdbc.update("INSERT INTO crew_member (crew_id, person_id, made_on) VALUES (?, ?, ?)", crewId, personId,
                    java.sql.Date.valueOf(sheets.today(sheetId)));
            return true;
        } catch (DuplicateKeyException e) {
            return false;
        }
    }
}
