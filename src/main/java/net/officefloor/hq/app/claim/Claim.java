package net.officefloor.hq.app.claim;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

/** A claim by a person on a sheet: an amount (negative only for a correction, which names the number of the
 * settled claim it corrects), a date, a note (may be empty)
 * a status (DRAFT to start), a number (given when opened), who opened it and a due (fixed when settled). */
@Entity
@Table(name = "claim")
public class Claim {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sheet_id", nullable = false)
    private String sheetId;

    @Column(name = "person_id", nullable = false)
    private Long personId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "claim_date", nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private String note;

    @Column(nullable = false)
    private String status = ClaimStatus.DRAFT.name();

    /** The sheet's number given when the claim was opened; null for a claim never opened; a voided claim keeps it. */
    @Column(name = "claim_number")
    private Integer number;

    /** The due fixed when the claim was settled; null while it is not settled or its grade had no rate then. */
    @Column(precision = 19, scale = 2)
    private BigDecimal due;

    /** The signed-in name that opened the claim; null while it was never opened. */
    @Column(name = "opened_by")
    private String openedBy;

    /** Whether the claim is bound by its person's last statement to the figure it had then. */
    @Column(nullable = false)
    private boolean stated;

    /** The figure the claim was bound to by its person's last statement; null while unbound or when it had none (n/a). */
    @Column(name = "stated_due", precision = 19, scale = 2)
    private BigDecimal statedDue;

    /** The number of the settled claim of the same person this correction corrects; null for any other claim. */
    @Column(name = "corrects")
    private Integer corrects;

    protected Claim() {
    }

    public Claim(String sheetId, Long personId, BigDecimal amount, LocalDate date, String note, Integer corrects) {
        this.corrects = corrects;
        this.sheetId = sheetId;
        this.personId = personId;
        this.amount = amount;
        this.date = date;
        this.note = note;
    }

    public Long getId() {
        return id;
    }

    public String getSheetId() {
        return sheetId;
    }

    public Long getPersonId() {
        return personId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public ClaimStatus getStatus() {
        return ClaimStatus.valueOf(status);
    }

    public void setStatus(ClaimStatus status) {
        this.status = status.name();
    }

    public Integer getNumber() {
        return number;
    }

    public void setNumber(Integer number) {
        this.number = number;
    }

    public BigDecimal getDue() {
        return due;
    }

    public void setDue(BigDecimal due) {
        this.due = due;
    }

    public String getOpenedBy() {
        return openedBy;
    }

    public void setOpenedBy(String openedBy) {
        this.openedBy = openedBy;
    }

    public Integer getCorrects() {
        return corrects;
    }

    public void setCorrects(Integer corrects) {
        this.corrects = corrects;
    }

    /** Whether the claim is a correction: it names a claim it corrects. */
    public boolean isCorrection() {
        return corrects != null;
    }

    public boolean isStated() {
        return stated;
    }

    public BigDecimal getStatedDue() {
        return statedDue;
    }

    /** Binds the claim to a figure (null: n/a), or unbinds it (live again). */
    public void bind(boolean stated, BigDecimal statedDue) {
        this.stated = stated;
        this.statedDue = stated ? statedDue : null;
    }
}
