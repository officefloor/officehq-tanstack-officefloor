package net.officefloor.hq.app.pool;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
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
        return jdbc.query("SELECT id, sheet_id, name FROM pool WHERE sheet_id = ? ORDER BY name",
                (rs, n) -> new Pool(rs.getLong("id"), rs.getString("sheet_id"), rs.getString("name")), sheetId);
    }

    /** The pool with this name on the sheet, or null when there is none. */
    public Pool find(String sheetId, String name) {
        List<Pool> found = jdbc.query("SELECT id, sheet_id, name FROM pool WHERE sheet_id = ? AND name = ?",
                (rs, n) -> new Pool(rs.getLong("id"), rs.getString("sheet_id"), rs.getString("name")), sheetId, name);
        return found.isEmpty() ? null : found.get(0);
    }

    public Pool save(String sheetId, String name) {
        jdbc.update("INSERT INTO pool (sheet_id, name) VALUES (?, ?)", sheetId, name);
        return find(sheetId, name);
    }

    /**
     * The members of every pool on the sheet, each pool's in the order they were added: people and member pools,
     * a person held when one of their holds covers {@code today}, a member active when its dates include it.
     */
    public List<PoolMember> members(String sheetId, LocalDate today) {
        return jdbc.query("SELECT pl.name AS pool, m.person_id, mp.name AS member_pool, COALESCE(p.name, mp.name) AS name,"
                + " m.share, m.from_date, m.to_date, p.retired_from, EXISTS (SELECT 1 FROM person_hold h WHERE h.person_id = m.person_id"
                + " AND h.from_date <= ? AND (h.to_date IS NULL OR h.to_date >= ?)) AS held FROM pool_member m"
                + " JOIN pool pl ON pl.id = m.pool_id LEFT JOIN person p ON p.id = m.person_id"
                + " LEFT JOIN pool mp ON mp.id = m.member_pool_id"
                + " WHERE pl.sheet_id = ? ORDER BY pl.name, m.id",
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
                }, Date.valueOf(today), Date.valueOf(today), sheetId);
    }

    public boolean isMember(Pool pool, long personId) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM pool_member WHERE pool_id = ? AND person_id = ?",
                Integer.class, pool.id(), personId);
        return n != null && n > 0;
    }

    public void addMember(Pool pool, long personId, BigDecimal share, LocalDate from, LocalDate to) {
        jdbc.update("INSERT INTO pool_member (pool_id, person_id, share, from_date, to_date) VALUES (?, ?, ?, ?, ?)",
                pool.id(), personId, share, sqlDate(from), sqlDate(to));
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
        jdbc.update("INSERT INTO pool_member (pool_id, member_pool_id, share, from_date, to_date) VALUES (?, ?, ?, ?, ?)",
                pool.id(), member.id(), share, sqlDate(from), sqlDate(to));
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
