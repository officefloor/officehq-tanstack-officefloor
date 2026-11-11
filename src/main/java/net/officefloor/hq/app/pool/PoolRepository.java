package net.officefloor.hq.app.pool;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.officefloor.hq.app.crew.CrewRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The pools store, kept per sheet. */
@Repository
public class PoolRepository {

    private final JdbcTemplate jdbc;

    public PoolRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's pools, by name. */
    public List<Pool> findAll(String sheetId) {
        return findAll(sheetId, null);
    }

    /** The sheet's pools, by name: as they stood at the end of {@code asOf} (null: now). */
    public List<Pool> findAll(String sheetId, LocalDate asOf) {
        return jdbc.query("SELECT id, sheet_id, name FROM pool WHERE sheet_id = ?"
                + " AND (CAST(? AS DATE) IS NULL OR added_on IS NULL OR added_on <= ?) ORDER BY name",
                (rs, n) -> new Pool(rs.getLong("id"), rs.getString("sheet_id"), rs.getString("name")),
                sheetId, sqlDate(asOf), sqlDate(asOf));
    }

    /** The pool with this name on the sheet, or null when there is none. */
    public Pool find(String sheetId, String name) {
        List<Pool> found = jdbc.query("SELECT id, sheet_id, name FROM pool WHERE sheet_id = ? AND name = ?",
                (rs, n) -> new Pool(rs.getLong("id"), rs.getString("sheet_id"), rs.getString("name")), sheetId, name);
        return found.isEmpty() ? null : found.get(0);
    }

    public Pool save(String sheetId, String name) {
        jdbc.update("INSERT INTO pool (sheet_id, name, added_on) VALUES (?, ?, (SELECT today FROM sheet WHERE id = ?))",
                sheetId, name, sheetId);
        return find(sheetId, name);
    }

    /**
     * The members of every pool on the sheet, each pool's in the order they were added: people and member pools,
     * a person held when one of their holds covers {@code today}, a member active when its dates include it.
     */
    public List<PoolMember> members(String sheetId, LocalDate today) {
        return members(sheetId, today, null);
    }

    /**
     * The members as they stood at the end of {@code asOf} (null: now): only those added, to pools added,
     * by then, and each person's retirement and holds as given by then.
     */
    public List<PoolMember> members(String sheetId, LocalDate today, LocalDate asOf) {
        Date on = sqlDate(asOf);
        String added = "(CAST(? AS DATE) IS NULL OR %1$s.added_on IS NULL OR %1$s.added_on <= ?)";
        return jdbc.query("SELECT pl.name AS pool, m.person_id, mp.name AS member_pool, COALESCE(p.name, mp.name) AS name,"
                + " m.share, m.from_date, m.to_date, CASE WHEN CAST(? AS DATE) IS NULL THEN p.retired_from ELSE"
                + " (SELECT r.from_date FROM person_retirement r WHERE r.person_id = m.person_id AND r.set_on <= ?"
                + " ORDER BY r.id DESC LIMIT 1) END AS retired_from,"
                + " EXISTS (SELECT 1 FROM person_hold h WHERE h.person_id = m.person_id"
                + " AND h.from_date <= ? AND (h.to_date IS NULL OR h.to_date >= ?)"
                + " AND " + added.formatted("h").replace("added_on", "made_on") + ") AS held FROM pool_member m"
                + " JOIN pool pl ON pl.id = m.pool_id LEFT JOIN person p ON p.id = m.person_id"
                + " LEFT JOIN pool mp ON mp.id = m.member_pool_id"
                + " WHERE pl.sheet_id = ? AND " + added.formatted("m") + " AND " + added.formatted("pl")
                + " AND (m.person_id IS NULL OR " + added.formatted("p") + ")"
                + " AND (m.member_pool_id IS NULL OR " + added.formatted("mp") + ")"
                + " ORDER BY pl.name, m.id",
                (rs, n) -> {
                    long personId = rs.getLong("person_id");
                    Long id = rs.wasNull() ? null : personId;
                    Date from = rs.getDate("from_date");
                    Date to = rs.getDate("to_date");
                    LocalDate fromDate = from == null ? null : from.toLocalDate();
                    LocalDate toDate = to == null ? null : to.toLocalDate();
                    return new PoolMember(sheetId, rs.getString("pool"), id,
                            rs.getString("member_pool"), rs.getString("name"), rs.getBigDecimal("share"),
                            rs.getDate("retired_from") != null, rs.getBoolean("held"), fromDate, toDate,
                            PoolMember.covers(fromDate, toDate, today));
                }, on, on, Date.valueOf(today), Date.valueOf(today), on, on, sheetId, on, on, on, on, on, on, on, on);
    }

    /**
     * The members of every crew on the sheet as a pool's (see {@link #crewShares}): each crew's lead first, then its
     * members in the order added, as they stood at the end of {@code asOf} (null: now); a person held when one of
     * their holds covers {@code today}. A crew's members have no dates, so are always active.
     */
    public List<PoolMember> crewMembers(String sheetId, LocalDate today, LocalDate asOf) {
        Date on = sqlDate(asOf);
        String added = "(CAST(? AS DATE) IS NULL OR %1$s.added_on IS NULL OR %1$s.added_on <= ?)";
        String flags = " CASE WHEN CAST(? AS DATE) IS NULL THEN p.retired_from ELSE"
                + " (SELECT r.from_date FROM person_retirement r WHERE r.person_id = p.id AND r.set_on <= ?"
                + " ORDER BY r.id DESC LIMIT 1) END AS retired_from,"
                + " EXISTS (SELECT 1 FROM person_hold h WHERE h.person_id = p.id"
                + " AND h.from_date <= ? AND (h.to_date IS NULL OR h.to_date >= ?)"
                + " AND " + added.formatted("h").replace("added_on", "made_on") + ") AS held";
        Date day = Date.valueOf(today);
        List<Object[]> rows = jdbc.query("SELECT pool, ord, person_id, name, retired_from, held FROM ("
                + "SELECT c.name AS pool, 0 AS ord, p.id AS person_id, p.name AS name," + flags
                + " FROM crew c JOIN person p ON p.id = c.lead_id WHERE c.sheet_id = ? AND " + added.formatted("c")
                + " UNION ALL SELECT c.name AS pool, m.id AS ord, p.id AS person_id, p.name AS name," + flags
                + " FROM (" + CrewRepository.MEMBERS_AS_OF + ") m JOIN crew c ON c.id = m.crew_id JOIN person p ON p.id = m.person_id"
                + " WHERE c.sheet_id = ? AND " + added.formatted("c") + " AND " + added.formatted("m")
                + ") x ORDER BY pool, ord",
                (rs, n) -> new Object[] { rs.getString("pool"), rs.getLong("person_id"), rs.getString("name"),
                        rs.getDate("retired_from") != null, rs.getBoolean("held") },
                on, on, day, day, on, on, sheetId, on, on,
                on, on, day, day, on, on, on, on, sheetId, on, on, on, on);
        Map<String, List<Object[]>> byCrew = new LinkedHashMap<>();
        for (Object[] row : rows) byCrew.computeIfAbsent((String) row[0], k -> new ArrayList<>()).add(row);
        List<PoolMember> members = new ArrayList<>();
        for (List<Object[]> crew : byCrew.values()) {
            List<BigDecimal> shares = crewShares(crew.size());
            for (int i = 0; i < crew.size(); i++) {
                Object[] row = crew.get(i);
                members.add(new PoolMember(sheetId, (String) row[0], (Long) row[1], null, (String) row[2],
                        shares.get(i), (Boolean) row[3], (Boolean) row[4], null, null, true));
            }
        }
        return members;
    }

    /**
     * A crew's {@code count} equal shares, its lead's first: 100.00 divided by the count, rounded half to even to two
     * decimals, the remainder (100.00 less the shares added up) to the lead, as patrons' shares are split.
     */
    static List<BigDecimal> crewShares(int count) {
        BigDecimal hundred = new BigDecimal("100.00");
        BigDecimal each = hundred.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_EVEN);
        List<BigDecimal> shares = new ArrayList<>();
        shares.add(hundred.subtract(each.multiply(BigDecimal.valueOf(count - 1))));
        for (int i = 1; i < count; i++) shares.add(each);
        return shares;
    }

    public boolean isMember(Pool pool, long personId) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM pool_member WHERE pool_id = ? AND person_id = ?",
                Integer.class, pool.id(), personId);
        return n != null && n > 0;
    }

    public void addMember(Pool pool, long personId, BigDecimal share, LocalDate from, LocalDate to) {
        jdbc.update("INSERT INTO pool_member (pool_id, person_id, share, from_date, to_date, added_on)"
                + " VALUES (?, ?, ?, ?, ?, (SELECT today FROM sheet WHERE id = ?))",
                pool.id(), personId, share, sqlDate(from), sqlDate(to), pool.sheetId());
    }

    /** The pool this pool is a member of, or null when it is a member of none. */
    public Pool parentOf(Pool pool) {
        List<Pool> found = jdbc.query("SELECT pl.id, pl.sheet_id, pl.name FROM pool_member m JOIN pool pl ON pl.id = m.pool_id"
                + " WHERE m.member_pool_id = ?",
                (rs, n) -> new Pool(rs.getLong("id"), rs.getString("sheet_id"), rs.getString("name")), pool.id());
        return found.isEmpty() ? null : found.get(0);
    }

    /** The pools that are members of this pool. */
    public List<Pool> memberPools(Pool pool) {
        return jdbc.query("SELECT mp.id, mp.sheet_id, mp.name FROM pool_member m JOIN pool mp ON mp.id = m.member_pool_id"
                + " WHERE m.pool_id = ?",
                (rs, n) -> new Pool(rs.getLong("id"), rs.getString("sheet_id"), rs.getString("name")), pool.id());
    }

    public void addMemberPool(Pool pool, Pool member, BigDecimal share, LocalDate from, LocalDate to) {
        jdbc.update("INSERT INTO pool_member (pool_id, member_pool_id, share, from_date, to_date, added_on)"
                + " VALUES (?, ?, ?, ?, ?, (SELECT today FROM sheet WHERE id = ?))",
                pool.id(), member.id(), share, sqlDate(from), sqlDate(to), pool.sheetId());
    }

    private static Date sqlDate(LocalDate day) {
        return day == null ? null : Date.valueOf(day);
    }

    /**
     * Move a merged person's memberships to the person they were merged into: their share is added to
     * the other's in a pool the other is already in.
     */
    public void mergeMembers(long fromId, long intoId) {
        jdbc.update("UPDATE pool_member m SET share = share + (SELECT f.share FROM pool_member f"
                + " WHERE f.pool_id = m.pool_id AND f.person_id = ?) WHERE m.person_id = ?"
                + " AND EXISTS (SELECT 1 FROM pool_member f WHERE f.pool_id = m.pool_id AND f.person_id = ?)",
                fromId, intoId, fromId);
        jdbc.update("DELETE FROM pool_member WHERE person_id = ? AND pool_id IN"
                + " (SELECT pool_id FROM pool_member WHERE person_id = ?)", fromId, intoId);
        jdbc.update("UPDATE pool_member SET person_id = ? WHERE person_id = ?", intoId, fromId);
    }
}
