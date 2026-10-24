package net.officefloor.hq.app.statement;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.time.LocalDate;

/** A statement taken for a person: their due-total then (two decimals, or n/a) and the sheet's today then. */
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

    /** How many of the person's claims counted, and so were bound; for the audit only. */
    @Transient
    private int claims;

    protected Statement() {
    }

    public Statement(String sheetId, Long personId, String dueTotal, LocalDate takenOn, int claims) {
        this.sheetId = sheetId;
        this.personId = personId;
        this.dueTotal = dueTotal;
        this.takenOn = takenOn;
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

    public int getClaims() {
        return claims;
    }
}
