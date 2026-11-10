package net.officefloor.hq.app.pool;

/** A pool member, as the screen reads it: the person's id and name, their share, and whether they are retired. */
public record PoolMemberResponse(String personId, String name, String share, boolean retired) {
    public static PoolMemberResponse from(PoolMember m) {
        return new PoolMemberResponse(String.valueOf(m.personId()), m.name(), m.share().toPlainString(), m.retired());
    }
}
