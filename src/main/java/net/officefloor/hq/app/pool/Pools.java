package net.officefloor.hq.app.pool;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The pool store: each sheet's pools, in the order added, and their members. */
@Repository
public class Pools {

    /** The shares of a balanced pool total exactly this. */
    public static final BigDecimal FULL = new BigDecimal("100.00");

    private final JdbcTemplate jdbc;

    public Pools(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's pools, in the order added, each with its members in the order added. A retired member
     *  (a retirement date set, whatever the date) counts 0.00 toward the total. While the sheet has a pot, each
     *  member of a balanced pool has a cut of it (see {@link #cuts}); otherwise no cut (""). */
    public List<PoolResponse> list(String sheetId) {
        Map<String, List<PoolResponse.Member>> members = new LinkedHashMap<>();
        for (String name : jdbc.queryForList("SELECT name FROM pool WHERE sheet_id = ? ORDER BY seq", String.class, sheetId)) {
            members.put(name, new ArrayList<>());
        }
        jdbc.query("SELECT m.pool_name, m.person_id, p.name, m.share, p.retired_from FROM pool_member m"
                + " JOIN person p ON p.id = m.person_id AND p.sheet_id = m.sheet_id"
                + " WHERE m.sheet_id = ? ORDER BY m.seq", rs -> {
                    List<PoolResponse.Member> list = members.get(rs.getString(1));
                    if (list != null) {
                        list.add(new PoolResponse.Member(String.valueOf(rs.getLong(2)), rs.getString(3),
                                rs.getBigDecimal(4).setScale(2).toPlainString(), rs.getDate(5) != null, ""));
                    }
                }, sheetId);
        List<BigDecimal> pots = jdbc.queryForList("SELECT pot FROM sheet WHERE id = ?", BigDecimal.class, sheetId);
        BigDecimal pot = pots.isEmpty() || pots.get(0) == null ? null : pots.get(0).setScale(2);
        List<PoolResponse> pools = new ArrayList<>();
        members.forEach((name, list) -> {
            BigDecimal total = BigDecimal.ZERO.setScale(2);
            for (PoolResponse.Member m : list) {
                if (!m.retired()) {
                    total = total.add(new BigDecimal(m.share()));
                }
            }
            boolean balanced = total.compareTo(FULL) == 0;
            pools.add(new PoolResponse(name, total.toPlainString(), balanced, balanced && pot != null ? cuts(list, pot) : list));
        });
        return pools;
    }

    /** The members with their cuts of the pot: the pot times their share divided by 100, rounded half to even to two
     *  decimals (a retired member cuts 0.00); the remainder, the pot minus the cuts added up, goes to the member with
     *  the largest share (the earliest added on a tie), so the cuts total exactly the pot. */
    static List<PoolResponse.Member> cuts(List<PoolResponse.Member> members, BigDecimal pot) {
        BigDecimal[] cuts = new BigDecimal[members.size()];
        BigDecimal sum = BigDecimal.ZERO.setScale(2);
        int largest = -1;
        for (int i = 0; i < members.size(); i++) {
            PoolResponse.Member m = members.get(i);
            BigDecimal share = new BigDecimal(m.share());
            cuts[i] = m.retired() ? BigDecimal.ZERO.setScale(2)
                    : pot.multiply(share).divide(FULL, 2, RoundingMode.HALF_EVEN);
            sum = sum.add(cuts[i]);
            if (!m.retired() && (largest < 0 || share.compareTo(new BigDecimal(members.get(largest).share())) > 0)) {
                largest = i;
            }
        }
        if (largest >= 0) {
            cuts[largest] = cuts[largest].add(pot.subtract(sum));
        }
        List<PoolResponse.Member> withCuts = new ArrayList<>();
        for (int i = 0; i < members.size(); i++) {
            PoolResponse.Member m = members.get(i);
            withCuts.add(new PoolResponse.Member(m.personId(), m.name(), m.share(), m.retired(), cuts[i].toPlainString()));
        }
        return withCuts;
    }

    /** Whether the sheet has a pool of the name. */
    public boolean contains(String sheetId, String name) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM pool WHERE sheet_id = ? AND name = ?", Integer.class, sheetId, name);
        return n != null && n > 0;
    }

    /** Whether the person is a member of the pool. */
    public boolean hasMember(String sheetId, String pool, long personId) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM pool_member WHERE sheet_id = ? AND pool_name = ? AND person_id = ?",
                Integer.class, sheetId, pool, personId);
        return n != null && n > 0;
    }

    /** Adds a pool to the sheet. */
    public void add(String sheetId, String name) {
        jdbc.update("INSERT INTO pool (sheet_id, name) VALUES (?, ?)", sheetId, name);
    }

    /** Adds the person to the pool with the share. */
    public void addMember(String sheetId, String pool, long personId, BigDecimal share) {
        jdbc.update("INSERT INTO pool_member (sheet_id, pool_name, person_id, share) VALUES (?, ?, ?, ?)",
                sheetId, pool, personId, share);
    }
}
