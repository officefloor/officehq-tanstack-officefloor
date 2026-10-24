package net.officefloor.hq.app.plan;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;

/** A claim a plan made, for one of its dates. */
@Entity
@Table(name = "plan_claim")
public class PlanClaim {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "plan_id", nullable = false)
    private Long planId;

    @Column(name = "on_date", nullable = false)
    private LocalDate onDate;

    @Column(name = "claim_id", nullable = false)
    private Long claimId;

    protected PlanClaim() {
    }

    public PlanClaim(Long planId, LocalDate onDate, Long claimId) {
        this.planId = planId;
        this.onDate = onDate;
        this.claimId = claimId;
    }

    public Long getPlanId() {
        return planId;
    }

    public LocalDate getOnDate() {
        return onDate;
    }

    public Long getClaimId() {
        return claimId;
    }
}
