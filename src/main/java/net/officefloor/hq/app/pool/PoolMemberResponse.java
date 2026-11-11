package net.officefloor.hq.app.pool;

/**
 * A pool member, as the screen reads it: the person's id ('' for a member pool) or the member pool's
 * name ('' for a person), the name, their share, whether they are retired, and their cut of the pool's
 * pot with two decimals ('' for none), its from- and to-dates ('' for open) and whether they
 * include the sheet's today. {@code periodCuts} is, for each period whose to-date is on or before the sheet's
 * today, or closed (by from-date), their cut as it stood at the end of that period, or as fixed when it closed
 * ('' for none).
 */
public record PoolMemberResponse(String personId, String memberPool, String name, String share, boolean retired,
        String cut, String from, String to, boolean active, java.util.List<PeriodCut> periodCuts) {
    public static PoolMemberResponse from(PoolMember m, java.math.BigDecimal cut) {
        return new PoolMemberResponse(m.personId() == null ? "" : String.valueOf(m.personId()),
                m.memberPool() == null ? "" : m.memberPool(), m.name(), m.share().toPlainString(), m.retired(),
                cut == null ? "" : cut.toPlainString(), m.from() == null ? "" : m.from().toString(),
                m.to() == null ? "" : m.to().toString(), m.active(), java.util.List.of());
    }

    /** This member, with its cuts at the end of each period. */
    public PoolMemberResponse withPeriodCuts(java.util.List<PeriodCut> periodCuts) {
        return new PoolMemberResponse(personId, memberPool, name, share, retired, cut, from, to, active, periodCuts);
    }
}
