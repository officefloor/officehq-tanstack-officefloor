package net.officefloor.hq.app;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A client the user tracks: a name and an email. Maps the {@code clients} table (Flyway
 * V1__clients.sql); the id is IDENTITY-generated on create.
 */
@Entity
@Table(name = "clients")
public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String email;

    // Whether the client has been archived (tucked away): an archived client is retained but drops
    // off the list and the search. New clients start not-archived (the column default,
    // V19__client_archived.sql).
    @Column(nullable = false)
    private boolean archived;

    // The currency this client is billed in — their money is shown in it everywhere the user sees it.
    // A short ISO code (USD, EUR, GBP); new clients start USD (the column default,
    // V33__client_currency.sql) until the user sets it.
    @Column(nullable = false)
    private String currency = "USD";

    protected Client() {
    }

    public Client(String name, String email) {
        this.name = name;
        this.email = email;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public boolean isArchived() {
        return archived;
    }

    /** The currency this client is billed in (a short ISO code: USD, EUR, GBP). */
    public String getCurrency() {
        return currency;
    }

    /** Set the currency this client is billed in — the currency their money is shown in everywhere. */
    public void setCurrency(String currency) {
        this.currency = currency;
    }

    /** Tuck this client away so it drops off the list and the search while the row is kept. */
    public void archive() {
        this.archived = true;
    }

    /** Bring a tucked-away client back so it returns to the list and the search. */
    public void restore() {
        this.archived = false;
    }

    /** Correct this client's details — its name and email — in place. */
    public void update(String name, String email) {
        this.name = name;
        this.email = email;
    }
}
