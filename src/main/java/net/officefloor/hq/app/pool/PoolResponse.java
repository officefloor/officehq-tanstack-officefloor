package net.officefloor.hq.app.pool;

import java.util.List;

/** A pool as the screen shows it: its name, its members, the total of their shares (a retired member's counting as
 *  given), whether that total is exactly 100.00, and whether it is a crew (whose members are its lead and members,
 *  and which takes no members through the pool's entry). */
public record PoolResponse(long id, String name, String totalShare, boolean balanced, boolean crew, List<Member> members) {

    /** A member: the person (or the member pool, the other null), its name, their share and their cut of the sheet's pot ("" when the pool is not
     *  balanced or there is no pot), and, for each period ended by the sheet's today, their cut as it stood at the
     *  end of it. */
    public record Member(Long personId, Long poolId, String name, String share, String cut, List<PeriodCut> periodCuts) {

        public Member(Long personId, Long poolId, String name, String share, String cut) {
            this(personId, poolId, name, share, cut, List.of());
        }

        public Member withPeriodCuts(List<PeriodCut> periodCuts) {
            return new Member(personId, poolId, name, share, cut, periodCuts);
        }
    }

    /** A member's cut as it stood at the end of a period (its to-date the sheet's today): "" when there was none. */
    public record PeriodCut(String period, String cut) {
    }
}
