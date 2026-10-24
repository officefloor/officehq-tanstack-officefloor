package net.officefloor.hq.app.statement;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** One binding of a person's claims, with the sheet's today when it was made: a statement's (each claim it bound,
 *  with its figure) or a cancel's (none bound). */
@Entity
@Table(name = "binding_log")
public class BindingLog {

    /** A claim bound, to its figure (null: n/a). */
    @Embeddable
    public static class Bound {

        @Column(name = "claim_id", nullable = false)
        private Long claimId;

        @Column(name = "stated_due", precision = 19, scale = 2)
        private BigDecimal statedDue;

        protected Bound() {
        }

        public Bound(Long claimId, BigDecimal statedDue) {
            this.claimId = claimId;
            this.statedDue = statedDue;
        }

        public Long getClaimId() {
            return claimId;
        }

        public BigDecimal getStatedDue() {
            return statedDue;
        }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sheet_id", nullable = false)
    private String sheetId;

    @Column(name = "person_id", nullable = false)
    private Long personId;

    @Column(name = "made_on", nullable = false)
    private LocalDate madeOn;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "binding_log_claim", joinColumns = @JoinColumn(name = "binding_id"))
    private List<Bound> bound = new ArrayList<>();

    protected BindingLog() {
    }

    public BindingLog(String sheetId, Long personId, LocalDate madeOn) {
        this.sheetId = sheetId;
        this.personId = personId;
        this.madeOn = madeOn;
    }

    public LocalDate getMadeOn() {
        return madeOn;
    }

    public List<Bound> getBound() {
        return bound;
    }

    public void bind(Long claimId, BigDecimal statedDue) {
        bound.add(new Bound(claimId, statedDue));
    }
}
