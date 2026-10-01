package net.officefloor.hq.app;

import java.util.regex.Pattern;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * POST /api/clients/update — correct a client's name and email, returning the updated row.
 * Wired by officefloor/rest/api/clients/update.POST.yml.
 *
 * The same rules as creating apply to the corrected values: a client must have a proper email
 * address, so we reject a blank or malformed email before persisting (mirroring the UI check in
 * edit.slot.tsx; the DB CHECK in V2__client_email_format.sql is the final guard). An email must
 * also stay unique across clients, so we reject one already held by ANOTHER client — a client
 * keeping its own email is fine (the UNIQUE constraint in V27__client_email_unique.sql is the final
 * guard). We reject a missing or unknown client id before writing anything so a bad request leaves
 * state untouched.
 */
public class UpdateClient {

    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    public void service(@RequestBody UpdateClientForm form, ClientRepository clients,
            ObjectResponse<ClientView> response) {
        Long id = form.getId();
        if (id == null) {
            throw new IllegalArgumentException("A client id is required");
        }
        Client client = clients.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("A valid client is required"));
        String email = form.getEmail() == null ? "" : form.getEmail().trim();
        if (!EMAIL.matcher(email).matches()) {
            throw new IllegalArgumentException("A valid email address is required");
        }
        if (clients.existsByEmailAndIdNot(email, id)) {
            throw new IllegalArgumentException("A client with this email already exists");
        }
        client.update(form.getName(), email);
        clients.save(client);
        response.send(ClientView.of(client));
    }
}
