package net.officefloor.hq.app;

import java.util.regex.Pattern;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * {@code POST /api/clients/update} — correct a client's name and/or email and return the updated
 * row. Wired by {@code officefloor/rest/api/clients/update.POST.yml}. The client must exist and the
 * email must stay valid and unique (a client may keep its own email); the update is audited through
 * {@link Audit} so the correction can be checked back later.
 */
public class ClientsUpdate {

    // Mirror of the UI check (ClientsPage.isValidEmail) and of ClientsPost, so a request that
    // bypasses the form is still rejected before any row is changed or audit record is written.
    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    public void service(@RequestBody UpdateClient body, ClientRepository clients, Audit audit,
            ObjectResponse<Client> response) {
        Long id = body.getId();
        Client client = id == null ? null : clients.findById(id).orElse(null);
        if (client == null) {
            throw new IllegalArgumentException("no such client");
        }
        String email = body.getEmail() == null ? "" : body.getEmail().trim();
        if (!EMAIL.matcher(email).matches()) {
            throw new IllegalArgumentException("a client requires a valid email address");
        }
        // Two clients may not share an email, but a client may keep its own — so exclude this row
        // from the uniqueness check. The UNIQUE constraint (V26__client_email_unique.sql) backs it.
        if (clients.existsByEmailAndIdNot(email, id)) {
            throw new IllegalArgumentException("a client with that email already exists");
        }
        client.setName(body.getName());
        client.setEmail(email);
        Client saved = clients.save(client);
        audit.record("CLIENT_UPDATED id=" + saved.getId() + " name=" + saved.getName());
        response.send(saved);
    }
}
