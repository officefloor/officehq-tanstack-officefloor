package net.officefloor.hq.app.pool;

/**
 * A pool member, as the screen reads it: the person's id and name, their share, whether they are
 * retired, and their cut of the pot with two decimals ('' for none).
 */
public record PoolMemberResponse(String personId, String name, String share, boolean retired, String cut) {
    public static PoolMemberResponse from(PoolMember m, java.math.BigDecimal cut) {
        return new PoolMemberResponse(String.valueOf(m.personId()), m.name(), m.share().toPlainString(), m.retired(),
                cut == null ? "" : cut.toPlainString());
    }
}
