package net.officefloor.hq.app.rollover;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** A sheet's last rollover, kept so the steward who made it can undo it on the same sheet-today: who made it, the
 *  sheet's today then, the old fiscal year (yyyy), the periods it generated and the opening balances it set. */
@Entity
@Table(name = "last_rollover")
public class LastRollover {

    /** An opening balance the rollover added to a person's for a period. */
    @Embeddable
    public static class Opening {
        @Column(name = "period_id", nullable = false)
        private Long periodId;

        @Column(name = "person_id", nullable = false)
        private Long personId;

        @Column(name = "amount", nullable = false, precision = 19, scale = 2)
        private BigDecimal amount;

        protected Opening() {
        }

        public Opening(Long periodId, Long personId, BigDecimal amount) {
            this.periodId = periodId;
            this.personId = personId;
            this.amount = amount;
        }

        public Long getPeriodId() {
            return periodId;
        }

        public Long getPersonId() {
            return personId;
        }

        public BigDecimal getAmount() {
            return amount;
        }
    }

    @Id
    @Column(name = "sheet_id")
    private String sheetId;

    @Column(name = "made_by", nullable = false)
    private String madeBy;

    @Column(name = "made_on", nullable = false)
    private LocalDate madeOn;

    @Column(name = "fiscal_year", nullable = false)
    private String fiscalYear;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "last_rollover_period", joinColumns = @JoinColumn(name = "sheet_id"))
    @Column(name = "period_id")
    private List<Long> periodIds = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "last_rollover_opening", joinColumns = @JoinColumn(name = "sheet_id"))
    private List<Opening> openings = new ArrayList<>();

    protected LastRollover() {
    }

    public LastRollover(String sheetId, String madeBy, LocalDate madeOn, String fiscalYear, List<Long> periodIds,
            List<Opening> openings) {
        this.sheetId = sheetId;
        this.madeBy = madeBy;
        this.madeOn = madeOn;
        this.fiscalYear = fiscalYear;
        this.periodIds.addAll(periodIds);
        this.openings.addAll(openings);
    }

    public String getSheetId() {
        return sheetId;
    }

    public String getMadeBy() {
        return madeBy;
    }

    public LocalDate getMadeOn() {
        return madeOn;
    }

    public String getFiscalYear() {
        return fiscalYear;
    }

    public List<Long> getPeriodIds() {
        return periodIds;
    }

    public List<Opening> getOpenings() {
        return openings;
    }
}
