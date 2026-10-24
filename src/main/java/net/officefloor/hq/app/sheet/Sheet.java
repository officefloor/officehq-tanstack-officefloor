package net.officefloor.hq.app.sheet;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** A sheet, chosen by its id; everything else in the app is kept per sheet. */
@Entity
@Table(name = "sheet")
public class Sheet {

    @Id
    private String id;

    protected Sheet() {
    }

    public Sheet(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
}
