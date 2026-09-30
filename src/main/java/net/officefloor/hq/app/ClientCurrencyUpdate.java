package net.officefloor.hq.app;

import java.util.Set;
import net.officefloor.server.http.HttpException;
import net.officefloor.server.http.HttpStatus;
import net.officefloor.web.HttpPathParameter;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * POST /api/clients/{clientId}/currency — set the currency a client pays in from {currency} and
 * return the saved row. Wired by {@code officefloor/rest/api/clients/{clientId}/currency.POST.yml}.
 *
 * <p>Only a currency the app knows how to render is accepted; an unknown code is rejected with 400 and
 * an unknown client id with 404. Setting the currency is an audited side-effect: one
 * {@code CLIENT_CURRENCY_SET id=<id> currency=<code>} record is appended per save (CLAUDE.md — audited
 * behaviour goes through {@link Audit}).
 */
public class ClientCurrencyUpdate {

    /** The currencies the app can render (mirrors the front-end's currency options). */
    private static final Set<String> CURRENCIES = Set.of("USD", "EUR");

    public void service(@HttpPathParameter("clientId") String clientId, @RequestBody NewCurrency body,
            ClientRepository clients, Audit audit, ObjectResponse<Client> response) {
        Client client = clients.findById(Long.valueOf(clientId))
                .orElseThrow(() -> new HttpException(HttpStatus.NOT_FOUND));
        String currency = body.getCurrency();
        if (currency == null || !CURRENCIES.contains(currency.trim())) {
            throw new HttpException(HttpStatus.BAD_REQUEST);
        }
        client.setCurrency(currency.trim());
        clients.save(client);
        audit.record("CLIENT_CURRENCY_SET id=" + client.getId() + " currency=" + client.getCurrency());
        response.send(client);
    }
}
