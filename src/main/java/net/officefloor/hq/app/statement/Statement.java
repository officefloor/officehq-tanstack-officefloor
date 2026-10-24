package net.officefloor.hq.app.statement;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.time.LocalDate;

/** A statement taken for a person: their due-total then (two decimals, or n/a), the date it was taken as of (the
 *  sheet's today then when none was given) and the sheet's today when it was taken. */
@Entity
@Table(name = "statement")
public class Statement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sheet_id", nullable = false)
    private String sheetId;

    @Column(name = "person_id", nullable = false)
    private Long personId;

    @Column(name = "due_total", nullable = false)
    private String dueTotal;

    @Column(name = "taken_on", nullable = false)
    private LocalDate takenOn;

    @Column(name = "made_on")
    private LocalDate madeOn;

    /** The date it was taken as of, when one was given; for the audit only. */
    @Transient
    private LocalDate asOf;

    /** How many of the person's claims counted, and so were bound; for the audit only. */
    @Transient
    private int claims;

    protected Statement() {
    }

    /** @param asOf the date it is taken as of; null for the sheet's today. */
    public Statement(String sheetId, Long personId, String dueTotal, LocalDate today, LocalDate asOf, int claims) {
        this.sheetId = sheetId;
        this.personId = personId;
        this.dueTotal = dueTotal;
        this.takenOn = asOf != null ? asOf : today;
        this.madeOn = today;
        this.asOf = asOf;
        this.claims = claims;
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

    public String getDueTotal() {
        return dueTotal;
    }

    public LocalDate getTakenOn() {
        return takenOn;
    }

    public LocalDate getMadeOn() {
        return madeOn;
    }

    public LocalDate getAsOf() {
        return asOf;
    }

    public int getClaims() {
        return claims;
    }
}
