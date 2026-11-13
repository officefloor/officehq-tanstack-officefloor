package net.officefloor.hq.app.pool;

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
        Date on = today == null ? null : Date.valueOf(today);
        Map<Long, Pool> pools = new LinkedHashMap<>();
        jdbc.query("SELECT id, name FROM pool WHERE sheet_id = ? ORDER BY id", rs -> {
            long id = rs.getLong("id");
            pools.put(id, new Pool(id, sheetId, rs.getString("name"), new ArrayList<>()));
        }, sheetId);
        jdbc.query("SELECT m.pool_id, m.person_id, m.member_pool_id, m.share, m.from_date, m.to_date, p.name,"
                + " p.retired_from, q.name AS pool_name,"
                + " CASE WHEN EXISTS (SELECT 1 FROM person_hold h WHERE h.person_id = m.person_id AND CAST(? AS DATE) IS NOT NULL"
                + " AND (h.from_date IS NULL OR h.from_date <= CAST(? AS DATE))"
                + " AND (h.to_date IS NULL OR h.to_date >= CAST(? AS DATE))) THEN 1 ELSE 0 END AS held"
                + " FROM pool_member m JOIN pool o ON o.id = m.pool_id LEFT JOIN person p ON p.id = m.person_id"
                + " LEFT JOIN pool q ON q.id = m.member_pool_id WHERE o.sheet_id = ? ORDER BY m.id",
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
                }, on, on, on, sheetId);
        return new ArrayList<>(pools.values());
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

    /** Adds a pool with no members. */
    public void add(String sheetId, String name) {
        jdbc.update("INSERT INTO pool (sheet_id, name) VALUES (?, ?)", sheetId, name);
    }

    /** Adds a member (a person or a pool) to the pool. */
    public void addMember(long poolId, PoolMember member) {
        jdbc.update("INSERT INTO pool_member (pool_id, person_id, member_pool_id, share, from_date, to_date)"
                + " VALUES (?, ?, ?, ?, ?, ?)", poolId, member.personId(), member.poolId(), member.share(),
                member.from() == null ? null : Date.valueOf(member.from()),
                member.to() == null ? null : Date.valueOf(member.to()));
    }
}
