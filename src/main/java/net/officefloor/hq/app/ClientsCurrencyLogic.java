package net.officefloor.hq.app;

import java.util.Set;
import org.springframework.web.bind.annotation.RequestBody;
import net.officefloor.server.http.HttpException;
import net.officefloor.server.http.HttpStatus;
import net.officefloor.web.ObjectResponse;

/**
 * {@code POST /api/clients/currency} — set the currency a client is billed in from an
 * {id, currency} body and return the saved row. Wired by
 * {@code officefloor/rest/api/clients/currency.POST.yml}. The client must exist (404 otherwise) and
 * the currency must be one the app supports (400 otherwise). Once set, the client's money is shown in
 * this currency everywhere it appears (their invoices, their statement, the dashboard and the top
 * clients list). One {@code CLIENT_CURRENCY_SET} audit record is appended so the change can be
 * checked back later through the audit file.
 */
public class ClientsCurrencyLogic {

    /** The currencies the app supports billing a client in. */
    private static final Set<String> SUPPORTED = Set.of("USD", "EUR");

    public void service(@RequestBody SetCurrency body, ClientRepository clients, Audit audit,
            ObjectResponse<Client> response) {
        Long id = body.getId();
        if (id == null) {
            throw new HttpException(HttpStatus.BAD_REQUEST, "A client id is required");
        }
        String currency = body.getCurrency() == null ? null : body.getCurrency().trim();
        if (currency == null || !SUPPORTED.contains(currency)) {
            throw new HttpException(HttpStatus.BAD_REQUEST, "A supported currency is required");
        }
        Client client = clients.findById(id)
                .orElseThrow(() -> new HttpException(HttpStatus.NOT_FOUND, "No such client"));
        client.setCurrency(currency);
        Client saved = clients.save(client);
        audit.record("CLIENT_CURRENCY_SET id=" + id + " currency=" + currency);
        response.send(saved);
    }

    /** Request body for setting a client's currency. */
    public static class SetCurrency {
        private Long id;
        private String currency;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getCurrency() {
            return currency;
        }

        public void setCurrency(String currency) {
            this.currency = currency;
        }
    }
}
