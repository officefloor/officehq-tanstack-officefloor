package net.officefloor.hq.app.pool;

/**
 * A pool member, as the screen reads it: the person's id ('' for a member pool) or the member pool's
 * name ('' for a person), the name, their share, whether they are retired, and their cut of the pool's
 * pot with two decimals ('' for none).
 */
public record PoolMemberResponse(String personId, String memberPool, String name, String share, boolean retired,
        String cut) {
    public static PoolMemberResponse from(PoolMember m, java.math.BigDecimal cut) {
        return new PoolMemberResponse(m.personId() == null ? "" : String.valueOf(m.personId()),
                m.memberPool() == null ? "" : m.memberPool(), m.name(), m.share().toPlainString(), m.retired(),
                cut == null ? "" : cut.toPlainString());
    }
}
