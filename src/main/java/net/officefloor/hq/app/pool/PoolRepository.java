package net.officefloor.hq.app.pool;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PoolRepository {

    private final JdbcTemplate jdbc;

    public PoolRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's pools with their members, each in the order they were added; no member is held. */
    public List<Pool> findAll(String sheetId) {
        return findAll(sheetId, null);
    }

    /**
     * The sheet's pools with their members, each in the order they were added; a person member is held when a hold of
     * theirs is in force on {@code today}, and a member is out when its dates do not include {@code today} (null: none
     * is held, none is out).
     */
    public List<Pool> findAll(String sheetId, LocalDate today) {
        return findAll(sheetId, today, false);
    }

    /**
     * The sheet's pools as {@link #findAll(String, LocalDate)} gives them; when {@code then}, as they stood at the end
     * of {@code today}: only the pools, members and holds added on or before it.
     */
    public List<Pool> findAll(String sheetId, LocalDate today, boolean then) {
        Date on = today == null ? null : Date.valueOf(today);
        Date upTo = then ? on : null;
        Map<Long, Pool> pools = new LinkedHashMap<>();
        jdbc.query("SELECT id, name FROM pool WHERE sheet_id = ?" + ADDED.formatted("") + " ORDER BY id", rs -> {
            long id = rs.getLong("id");
            pools.put(id, new Pool(id, sheetId, rs.getString("name"), new ArrayList<>()));
        }, sheetId, upTo, upTo);
        jdbc.query("SELECT m.pool_id, m.person_id, m.member_pool_id, m.share, m.from_date, m.to_date, p.name,"
                + " p.retired_from, q.name AS pool_name,"
                + " CASE WHEN EXISTS (SELECT 1 FROM person_hold h WHERE h.person_id = m.person_id AND CAST(? AS DATE) IS NOT NULL"
                + " AND (h.from_date IS NULL OR h.from_date <= CAST(? AS DATE))"
                + " AND (h.to_date IS NULL OR h.to_date >= CAST(? AS DATE))" + ADDED.formatted("h.") + ") THEN 1 ELSE 0 END AS held"
                + " FROM pool_member m JOIN pool o ON o.id = m.pool_id LEFT JOIN person p ON p.id = m.person_id"
                + " LEFT JOIN pool q ON q.id = m.member_pool_id WHERE o.sheet_id = ?" + ADDED.formatted("o.")
                + ADDED.formatted("m.") + " ORDER BY m.id",
                rs -> {
                    Pool pool = pools.get(rs.getLong("pool_id"));
                    Date fromDate = rs.getDate("from_date");
                    Date toDate = rs.getDate("to_date");
                    LocalDate from = fromDate == null ? null : fromDate.toLocalDate();
                    LocalDate to = toDate == null ? null : toDate.toLocalDate();
                    boolean out = !PoolMember.includes(from, to, today);
                    long personId = rs.getLong("person_id");
                    if (rs.wasNull()) {
                        pool.members().add(new PoolMember(null, rs.getLong("member_pool_id"),
                                rs.getString("pool_name"), rs.getBigDecimal("share"), false, false, from, to, out));
                    } else {
                        pool.members().add(new PoolMember(personId, null, rs.getString("name"),
                                rs.getBigDecimal("share"), rs.getDate("retired_from") != null, rs.getInt("held") == 1,
                                from, to, out));
                    }
                }, on, on, on, upTo, upTo, sheetId, upTo, upTo, upTo, upTo);
        return new ArrayList<>(pools.values());
    }

    /**
     * The sheet's crews as pools, in the order the crews were added: each named as its crew, with id the crew's id
     * negated (so it is never a stored pool's), and the lead then the members as its members at equal shares of 100.00
     * (each rounded half to even to two decimals, the remainder on the lead — on the first member when it has none — as patrons' shares are split); a person
     * member is held when a hold of theirs is in force on {@code today} (null: none is held).
     */
    public List<Pool> crewPools(String sheetId, LocalDate today) {
        return crewPools(sheetId, today, false);
    }

    /**
     * The sheet's crews as pools as {@link #crewPools(String, LocalDate)} gives them; when {@code then}, as they stood at
     * the end of {@code today}: only the crews, members and holds added on or before it.
     */
    public List<Pool> crewPools(String sheetId, LocalDate today, boolean then) {
        Date on = today == null ? null : Date.valueOf(today);
        Date upTo = then ? on : null;
        Map<Long, Pool> pools = new LinkedHashMap<>();
        String held = " CASE WHEN EXISTS (SELECT 1 FROM person_hold h WHERE h.person_id = p.id AND CAST(? AS DATE) IS NOT NULL"
                + " AND (h.from_date IS NULL OR h.from_date <= CAST(? AS DATE))"
                + " AND (h.to_date IS NULL OR h.to_date >= CAST(? AS DATE))" + ADDED.formatted("h.") + ") THEN 1 ELSE 0 END AS held";
        jdbc.query("SELECT c.id, c.name, p.id AS person_id, p.name AS person_name, p.retired_from," + held
                + " FROM crew c LEFT JOIN person p ON p.id = c.lead_id WHERE c.sheet_id = ?" + ADDED.formatted("c.")
                + " ORDER BY c.id", rs -> {
                    long id = rs.getLong("id");
                    Pool pool = new Pool(-id, sheetId, rs.getString("name"), new ArrayList<>());
                    rs.getLong("person_id");
                    if (!rs.wasNull()) {
                        pool.members().add(crewMember(rs));
                    }
                    pools.put(id, pool);
                }, on, on, on, upTo, upTo, sheetId, upTo, upTo);
        jdbc.query("SELECT m.crew_id, p.id AS person_id, p.name AS person_name, p.retired_from," + held
                + " FROM crew_member m JOIN person p ON p.id = m.person_id WHERE m.sheet_id = ?" + ADDED.formatted("m.")
                + " ORDER BY m.id",
                rs -> {
                    Pool crew = pools.get(rs.getLong("crew_id"));
                    if (crew != null) {
                        crew.members().add(crewMember(rs));
                    }
                }, on, on, on, upTo, upTo, sheetId, upTo, upTo);
        List<Pool> list = new ArrayList<>();
        for (Pool pool : pools.values()) {
            int n = pool.members().size();
            if (n == 0) {
                list.add(pool);
                continue;
            }
            BigDecimal share = HUNDRED.divide(BigDecimal.valueOf(n), 2, RoundingMode.HALF_EVEN);
            BigDecimal lead = HUNDRED.subtract(share.multiply(BigDecimal.valueOf(n))).add(share);
            List<PoolMember> members = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                PoolMember m = pool.members().get(i);
                members.add(new PoolMember(m.personId(), null, m.name(), i == 0 ? lead : share, m.retired(), m.held(),
                        null, null, false));
            }
            list.add(new Pool(pool.id(), sheetId, pool.name(), members));
        }
        return list;
    }

    /**
     * A condition that the row (its columns under the prefix given) was added on or before a date: its two parameters
     * are the date (null: any row).
     */
    private static final String ADDED = " AND (CAST(? AS DATE) IS NULL OR %1$sadded_on IS NULL OR %1$sadded_on <= CAST(? AS DATE))";

    private static final BigDecimal HUNDRED = new BigDecimal("100.00");

    /** A crew's lead or member as a pool member, its share unset. */
    private static PoolMember crewMember(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new PoolMember(rs.getLong("person_id"), null, rs.getString("person_name"), null,
                rs.getDate("retired_from") != null, rs.getInt("held") == 1, null, null, false);
    }

    /** The sheet's pool with this id, or null. */
    public Pool find(String sheetId, long id) {
        return findAll(sheetId).stream().filter(p -> p.id() == id).findFirst().orElse(null);
    }

    /** Whether the sheet has a pool of this name. */
    public boolean nameTaken(String sheetId, String name) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM pool WHERE sheet_id = ? AND name = ?", Integer.class,
                sheetId, name);
        return n != null && n > 0;
    }

    /** Whether the sheet has a crew of this name (a crew is also a pool, under its name). */
    public boolean crewNamed(String sheetId, String name) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM crew WHERE sheet_id = ? AND name = ?", Integer.class,
                sheetId, name);
        return n != null && n > 0;
    }

    /** Adds a pool with no members. */
    public void add(String sheetId, String name) {
        jdbc.update("INSERT INTO pool (sheet_id, name, added_on) SELECT ?, ?, today FROM sheet WHERE id = ?", sheetId,
                name, sheetId);
    }

    /** Adds a member (a person or a pool) to the pool. */
    public void addMember(long poolId, PoolMember member) {
        jdbc.update("INSERT INTO pool_member (pool_id, person_id, member_pool_id, share, from_date, to_date, added_on)"
                + " SELECT ?, ?, ?, ?, ?, ?, s.today FROM pool o JOIN sheet s ON s.id = o.sheet_id WHERE o.id = ?", poolId,
                member.personId(), member.poolId(), member.share(),
                member.from() == null ? null : Date.valueOf(member.from()),
                member.to() == null ? null : Date.valueOf(member.to()), poolId);
    }

    /** The key a member's fixed cut is kept under: its pool, and the person or pool it is. */
    public static String cutKey(long poolId, Long personId, Long memberPoolId) {
        return poolId + ":" + (personId == null ? "" : personId) + ":" + (memberPoolId == null ? "" : memberPoolId);
    }

    /** Fix the period's cuts (by {@link #cutKey}; "": none), replacing any fixed before. */
    public void saveFixedCuts(String sheetId, String period, Map<String, String> cuts) {
        clearFixedCuts(sheetId, period);
        cuts.forEach((key, cut) -> {
            String[] k = key.split(":", -1);
            jdbc.update("INSERT INTO period_pool_cut (sheet_id, period_name, pool_id, person_id, member_pool_id, cut)"
                    + " VALUES (?, ?, ?, ?, ?, ?)", sheetId, period, Long.parseLong(k[0]),
                    k[1].isEmpty() ? null : Long.parseLong(k[1]), k[2].isEmpty() ? null : Long.parseLong(k[2]), cut);
        });
    }

    /** Make the period's cuts live again. */
    public void clearFixedCuts(String sheetId, String period) {
        jdbc.update("DELETE FROM period_pool_cut WHERE sheet_id = ? AND period_name = ?", sheetId, period);
    }

    /** The sheet's fixed cuts: by period name, each by {@link #cutKey}. */
    public Map<String, Map<String, String>> fixedCuts(String sheetId) {
        Map<String, Map<String, String>> fixed = new LinkedHashMap<>();
        jdbc.query("SELECT period_name, pool_id, person_id, member_pool_id, cut FROM period_pool_cut WHERE sheet_id = ?",
                rs -> {
                    fixed.computeIfAbsent(rs.getString("period_name"), k -> new LinkedHashMap<>()).put(
                            cutKey(rs.getLong("pool_id"), rs.getObject("person_id", Long.class),
                                    rs.getObject("member_pool_id", Long.class)), rs.getString("cut"));
                }, sheetId);
        return fixed;
    }
}
