package net.officefloor.hq.app.person;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** One of a person's aliases: another name they go by, unique on the sheet among names and aliases, ignoring case. */
@Entity
@Table(name = "person_alias")
public class PersonAlias {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sheet_id", nullable = false)
    private String sheetId;

    @Column(name = "person_id", nullable = false)
    private Long personId;

    @Column(nullable = false)
    private String alias;

    protected PersonAlias() {
    }

    public PersonAlias(String sheetId, Long personId, String alias) {
        this.sheetId = sheetId;
        this.personId = personId;
        this.alias = alias;
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

    public String getAlias() {
        return alias;
    }
}
