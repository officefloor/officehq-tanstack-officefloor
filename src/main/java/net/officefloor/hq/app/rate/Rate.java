package net.officefloor.hq.app.rate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** A sheet's rate for a grade: a percent with two decimals, from a date on (null: from the beginning of time),
 *  through a to-date (null: no end); the newest of its versions, numbered from 1 (older ones are in {@link RateVersion}). */
@Entity
@Table(name = "rate")
public class Rate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sheet_id", nullable = false)
    private String sheetId;

    @Column(nullable = false)
    private String grade;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal pct;

    @Column(name = "from_date")
    private LocalDate fromDate;

    @Column(name = "to_date")
    private LocalDate toDate;

    @Column(nullable = false)
    private int version = 1;

    /** Its log lines, in the order set, when looked up with them (empty: it applies to every claim as it is). */
    @Transient
    private List<RateLog> layers = new ArrayList<>();

    protected Rate() {
    }

    public Rate(String sheetId, String grade, BigDecimal pct, LocalDate fromDate, LocalDate toDate) {
        this.sheetId = sheetId;
        this.grade = grade;
        this.pct = pct;
        this.fromDate = fromDate;
        this.toDate = toDate;
    }

    public Long getId() {
        return id;
    }

    public String getSheetId() {
        return sheetId;
    }

    public String getGrade() {
        return grade;
    }

    public BigDecimal getPct() {
        return pct;
    }

    public void setPct(BigDecimal pct) {
        this.pct = pct;
    }

    public LocalDate getFromDate() {
        return fromDate;
    }

    /** The last date it applies on (null: no end). */
    public LocalDate getToDate() {
        return toDate;
    }

    public void setToDate(LocalDate toDate) {
        this.toDate = toDate;
    }

    /** Its version number: how many versions it has had (1 when first set). */
    public int getVersion() {
        return version;
    }

    /** Makes it the next version: the caller keeps the one it replaces. */
    public void nextVersion() {
        version++;
    }

    /** Whether it has ended by a date: it has a to-date before it. */
    public boolean endedBy(LocalDate on) {
        return toDate != null && toDate.isBefore(on);
    }

    /** Its log lines, in the order set (empty when not looked up with them). */
    public List<RateLog> getLayers() {
        return layers;
    }

    /** Adds one of its log lines, after those already added. */
    public void addLayer(RateLog line) {
        layers.add(line);
    }

    /** Keeps only its log lines among those given (by grade and from-date), in their order. */
    public void setLayers(List<RateLog> lines) {
        layers = new ArrayList<>(lines.stream().filter(l -> l.getGrade().equals(grade)
                && Objects.equals(l.getFromDate(), fromDate)).toList());
    }

    /** Itself as it applies to a claim (null id: one not yet added, which takes every rate): the last of its log lines
     *  that applies to it — one not forward-only, or forward-only saved before the claim was added; empty when none
     *  does (it had not been set for the claim). As it is when it has no log lines. Only worked out, never saved. */
    public Optional<Rate> forClaim(Long claimId) {
        if (layers.isEmpty() || claimId == null) {
            return Optional.of(this);
        }
        RateLog applies = null;
        for (RateLog line : layers) {
            if (line.getForwardAfter() == null || claimId > line.getForwardAfter()) {
                applies = line;
            }
        }
        if (applies == layers.get(layers.size() - 1)) {
            return Optional.of(this);
        }
        return applies == null ? Optional.empty()
                : Optional.of(new Rate(sheetId, grade, applies.getPct(), fromDate, applies.getToDate()));
    }
}
