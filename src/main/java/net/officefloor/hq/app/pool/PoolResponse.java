package net.officefloor.hq.app.pool;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** A pool as the screen shows it: its members in the order they were added, the total of their shares (a member
 *  retired by the date counts 0.00) and whether it is balanced (the total is exactly 100.00). */
public record PoolResponse(long id, String name, List<Member> members, String totalShare, boolean balanced) {

    /** A member: the person, their share, and whether they are retired by the date. */
    public record Member(long personId, String name, String share, boolean retired) {
    }

    private static final BigDecimal FULL = new BigDecimal("100.00");

    /** The pool with its members; retiredFrom gives each person's retirement date (absent: not retired) and on is
     *  the date it is judged by (null: none retired). */
    public static PoolResponse from(Pool pool, List<PoolMember> members, Map<Long, LocalDate> retiredFrom,
            LocalDate on) {
        BigDecimal total = BigDecimal.ZERO.setScale(2);
        List<Member> shown = new ArrayList<>();
        for (PoolMember m : members) {
            LocalDate from = retiredFrom.get(m.personId());
            boolean retired = on != null && from != null && !from.isAfter(on);
            if (!retired) {
                total = total.add(m.share());
            }
            shown.add(new Member(m.personId(), m.name(), m.share().toPlainString(), retired));
        }
        return new PoolResponse(pool.id(), pool.name(), shown, total.toPlainString(), total.compareTo(FULL) == 0);
    }

    public static PoolResponse from(Pool pool, List<PoolMember> members, LocalDate on) {
        return from(pool, members, Map.of(), on);
    }
}
