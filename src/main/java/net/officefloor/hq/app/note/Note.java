package net.officefloor.hq.app.note;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;

/** A sheet's note: a text, the user name who added it, and the sheet's today when it was added. */
@Entity
@Table(name = "sheet_note")
public class Note {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sheet_id", nullable = false)
    private String sheetId;

    @Column(name = "note_text", nullable = false)
    private String text;

    @Column(name = "by_user", nullable = false)
    private String byUser;

    @Column(name = "on_date", nullable = false)
    private LocalDate onDate;

    protected Note() {
    }

    public Note(String sheetId, String text, String byUser, LocalDate onDate) {
        this.sheetId = sheetId;
        this.text = text;
        this.byUser = byUser;
        this.onDate = onDate;
    }

    public Long getId() {
        return id;
    }

    public String getSheetId() {
        return sheetId;
    }

    public String getText() {
        return text;
    }

    public String getByUser() {
        return byUser;
    }

    public LocalDate getOnDate() {
        return onDate;
    }
}
