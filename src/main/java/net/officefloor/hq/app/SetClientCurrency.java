package net.officefloor.hq.app;

import java.util.Set;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * POST /api/clients/currency — set the currency a client is billed in, returning the updated row.
 * Wired by officefloor/rest/api/clients/currency.POST.yml.
 *
 * The currency must be one the app can render (USD, EUR or GBP — the set the DB CHECK in
 * V33__client_currency.sql guards), so we reject anything else before persisting. We reject a
 * missing or unknown client id before writing anything so a bad request leaves state untouched.
 */
public class SetClientCurrency {

    private static final Set<String> ALLOWED = Set.of("USD", "EUR", "GBP");

    public void service(@RequestBody SetClientCurrencyForm form, ClientRepository clients,
            ObjectResponse<ClientView> response) {
        Long id = form.getId();
        if (id == null) {
            throw new IllegalArgumentException("A client id is required");
        }
        String currency = form.getCurrency() == null ? "" : form.getCurrency().trim();
        if (!ALLOWED.contains(currency)) {
            throw new IllegalArgumentException("A supported currency is required");
        }
        Client client = clients.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("A valid client is required"));
        client.setCurrency(currency);
        clients.save(client);
        response.send(ClientView.of(client));
    }
}
