package net.officefloor.hq.app.pool;

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

    /** The sheet's pools with their members, each in the order they were added. */
    public List<Pool> findAll(String sheetId) {
        Map<Long, Pool> pools = new LinkedHashMap<>();
        jdbc.query("SELECT id, name FROM pool WHERE sheet_id = ? ORDER BY id", rs -> {
            long id = rs.getLong("id");
            pools.put(id, new Pool(id, sheetId, rs.getString("name"), new ArrayList<>()));
        }, sheetId);
        jdbc.query("SELECT m.pool_id, m.person_id, m.member_pool_id, m.share, p.name, p.retired_from, q.name AS pool_name"
                + " FROM pool_member m JOIN pool o ON o.id = m.pool_id LEFT JOIN person p ON p.id = m.person_id"
                + " LEFT JOIN pool q ON q.id = m.member_pool_id WHERE o.sheet_id = ? ORDER BY m.id",
                rs -> {
                    Pool pool = pools.get(rs.getLong("pool_id"));
                    long personId = rs.getLong("person_id");
                    if (rs.wasNull()) {
                        pool.members().add(new PoolMember(null, rs.getLong("member_pool_id"),
                                rs.getString("pool_name"), rs.getBigDecimal("share"), false));
                    } else {
                        pool.members().add(new PoolMember(personId, null, rs.getString("name"),
                                rs.getBigDecimal("share"), rs.getDate("retired_from") != null));
                    }
                }, sheetId);
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
        jdbc.update("INSERT INTO pool_member (pool_id, person_id, member_pool_id, share) VALUES (?, ?, ?, ?)", poolId,
                member.personId(), member.poolId(), member.share());
    }
}
