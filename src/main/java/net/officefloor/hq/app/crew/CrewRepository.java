package net.officefloor.hq.app.crew;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/** The crews store, kept per sheet. */
@Repository
public class CrewRepository {

    private static final String SELECT = "SELECT c.id, c.sheet_id, c.name, c.lead_id, p.name AS lead FROM crew c"
            + " LEFT JOIN person p ON p.id = c.lead_id";

    private static final RowMapper<Crew> CREW = (rs, n) -> new Crew(rs.getLong("id"), rs.getString("sheet_id"),
            rs.getString("name"), rs.getObject("lead_id", Long.class), rs.getString("lead"));

    /**
     * The crew members as they stood at the end of the date bound twice (null: now): the members now, and
     * those a merge took away after that date. The row's added_on still limits it to members added by then.
     */
    public static final String MEMBERS_AS_OF = "SELECT id, crew_id, person_id, added_on FROM crew_member"
            + " UNION ALL SELECT id, crew_id, person_id, added_on FROM crew_member_removed"
            + " WHERE CAST(? AS DATE) IS NOT NULL AND removed_on > ?";

    private final JdbcTemplate jdbc;

    public CrewRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's crews, by name: as they stood at the end of {@code asOf} (null: now). */
    public List<Crew> findAll(String sheetId, LocalDate asOf) {
        Date on = sqlDate(asOf);
        return jdbc.query(SELECT + " WHERE c.sheet_id = ?"
                + " AND (CAST(? AS DATE) IS NULL OR c.added_on IS NULL OR c.added_on <= ?) ORDER BY c.name",
                CREW, sheetId, on, on);
    }

    /** The crew with this name on the sheet, or null when there is none. */
    public Crew find(String sheetId, String name) {
        List<Crew> found = jdbc.query(SELECT + " WHERE c.sheet_id = ? AND c.name = ?", CREW, sheetId, name);
        return found.isEmpty() ? null : found.get(0);
    }

    public Crew save(String sheetId, String name, long leadId) {
        jdbc.update("INSERT INTO crew (sheet_id, name, lead_id, added_on) VALUES (?, ?, ?, (SELECT today FROM sheet WHERE id = ?))",
                sheetId, name, leadId, sheetId);
        return find(sheetId, name);
    }

    /** The members of every crew on the sheet, each crew's in the order they were added, as they stood at the end of {@code asOf} (null: now). */
    public List<CrewMember> members(String sheetId, LocalDate asOf) {
        Date on = sqlDate(asOf);
        return jdbc.query("SELECT c.name AS crew, m.person_id, p.name FROM (" + MEMBERS_AS_OF + ") m"
                + " JOIN crew c ON c.id = m.crew_id JOIN person p ON p.id = m.person_id WHERE c.sheet_id = ?"
                + " AND (CAST(? AS DATE) IS NULL OR m.added_on IS NULL OR m.added_on <= ?) ORDER BY c.name, m.id",
                (rs, n) -> new CrewMember(sheetId, rs.getString("crew"), rs.getLong("person_id"), rs.getString("name")),
                on, on, sheetId, on, on);
    }

    public boolean isMember(Crew crew, long personId) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM crew_member WHERE crew_id = ? AND person_id = ?",
                Integer.class, crew.id(), personId);
        return n != null && n > 0;
    }

    public void addMember(Crew crew, long personId) {
        jdbc.update("INSERT INTO crew_member (crew_id, person_id, added_on) VALUES (?, ?, (SELECT today FROM sheet WHERE id = ?))",
                crew.id(), personId, crew.sheetId());
    }

    private static Date sqlDate(LocalDate day) {
        return day == null ? null : Date.valueOf(day);
    }

    /**
     * Move a merged person's crew memberships to the person they were merged into, and a crew they lead
     * to the other's lead: dropped from a crew the other is already in or leads.
     */
    public void mergeMembers(long fromId, long intoId) {
        // Every membership the merge takes away is kept, so the crews still read as they stood before it.
        jdbc.update("INSERT INTO crew_member_removed (id, crew_id, person_id, added_on, removed_on)"
                + " SELECT m.id, m.crew_id, m.person_id, m.added_on, s.today FROM crew_member m"
                + " JOIN crew c ON c.id = m.crew_id JOIN sheet s ON s.id = c.sheet_id"
                + " WHERE m.person_id = ? OR (m.person_id = ? AND c.lead_id = ?)", fromId, intoId, fromId);
        jdbc.update("UPDATE crew SET lead_id = ? WHERE lead_id = ?", intoId, fromId);
        jdbc.update("DELETE FROM crew_member WHERE person_id IN (?, ?) AND crew_id IN"
                + " (SELECT id FROM crew WHERE lead_id = ?)", fromId, intoId, intoId);
        jdbc.update("DELETE FROM crew_member WHERE person_id = ? AND crew_id IN"
                + " (SELECT crew_id FROM crew_member WHERE person_id = ?)", fromId, intoId);
        jdbc.update("UPDATE crew_member SET person_id = ?, added_on = (SELECT s.today FROM sheet s JOIN crew c"
                + " ON c.sheet_id = s.id WHERE c.id = crew_member.crew_id) WHERE person_id = ?", intoId, fromId);
    }
}
