package net.officefloor.hq.app;

import java.util.Set;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * {@code POST /api/clients/currency} — set the currency a client is billed in and return the updated
 * row. Wired by {@code officefloor/rest/api/clients/currency.POST.yml}. The client must exist and the
 * currency must be one the app supports ({@code USD} or {@code EUR}); the change is audited through
 * {@link Audit} so it can be checked back later, mirroring {@link ClientsUpdate}.
 */
public class ClientsSetCurrency {

    /** The currencies the app can render and bill in — the same set the UI's select offers. */
    private static final Set<String> SUPPORTED = Set.of("USD", "EUR");

    public void service(@RequestBody SetClientCurrency body, ClientRepository clients, Audit audit,
            ObjectResponse<Client> response) {
        Long id = body.getId();
        Client client = id == null ? null : clients.findById(id).orElse(null);
        if (client == null) {
            throw new IllegalArgumentException("no such client");
        }
        String currency = body.getCurrency() == null ? "" : body.getCurrency().trim().toUpperCase();
        if (!SUPPORTED.contains(currency)) {
            throw new IllegalArgumentException("unsupported currency");
        }
        client.setCurrency(currency);
        Client saved = clients.save(client);
        audit.record("CLIENT_CURRENCY_SET id=" + saved.getId() + " currency=" + saved.getCurrency());
        response.send(saved);
    }
}
