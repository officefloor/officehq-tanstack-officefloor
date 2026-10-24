package net.officefloor.hq.app.pool;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

/** A pool's member: a person of the sheet, in the pool at most once, or another pool of the sheet (a member of at
 *  most one pool), with a share percent and, optionally, the dates it runs from and to. */
@Entity
@Table(name = "pool_member")
public class PoolMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pool_id", nullable = false)
    private Long poolId;

    @Column(name = "person_id")
    private Long personId;

    @Column(name = "member_pool_id")
    private Long memberPoolId;

    @Column(name = "share", nullable = false)
    private BigDecimal share;

    @Column(name = "from_date")
    private LocalDate fromDate;

    @Column(name = "to_date")
    private LocalDate toDate;

    protected PoolMember() {
    }

    public PoolMember(Long poolId, Long personId, BigDecimal share) {
        this.poolId = poolId;
        this.personId = personId;
        this.share = share;
    }

    /** A member that is another pool. */
    public static PoolMember ofPool(Long poolId, Long memberPoolId, BigDecimal share) {
        PoolMember member = new PoolMember(poolId, null, share);
        member.memberPoolId = memberPoolId;
        return member;
    }

    public Long getId() {
        return id;
    }

    public Long getPoolId() {
        return poolId;
    }

    public Long getPersonId() {
        return personId;
    }

    public Long getMemberPoolId() {
        return memberPoolId;
    }

    public BigDecimal getShare() {
        return share;
    }

    public LocalDate getFromDate() {
        return fromDate;
    }

    public LocalDate getToDate() {
        return toDate;
    }

    /** Sets the dates the membership runs from and to (either null for open). */
    public PoolMember during(LocalDate from, LocalDate to) {
        this.fromDate = from;
        this.toDate = to;
        return this;
    }

    /** Whether the membership's dates include the day (always when the day is unknown). */
    public boolean includes(LocalDate day) {
        if (day == null) return true;
        return (fromDate == null || !day.isBefore(fromDate)) && (toDate == null || !day.isAfter(toDate));
    }

    public void setPersonId(Long personId) {
        this.personId = personId;
    }

    public void setShare(BigDecimal share) {
        this.share = share;
    }
}
