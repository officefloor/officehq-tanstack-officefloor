package net.officefloor.hq.app.pool;

import java.util.List;

/** A pool as the screen shows it: its name, its members, the total of their shares (a retired member's counting as
 *  0.00) and whether that total is exactly 100.00. */
public record PoolResponse(long id, String name, String totalShare, boolean balanced, List<Member> members) {

    /** A member: the person, their name and their share. */
    public record Member(long personId, String name, String share) {
    }
}
