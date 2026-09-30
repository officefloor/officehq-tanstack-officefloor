package net.officefloor.hq.app;

import java.util.regex.Pattern;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * {@code POST /api/clients} — add a client (name + email) and return the created row. Wired by
 * {@code officefloor/rest/api/clients.POST.yml}. The create is audited through {@link Audit}.
 */
public class ClientsPost {

    // Every client must carry a proper email. Mirror of the UI check (ClientsPage.isValidEmail) so a
    // request that bypasses the form is still rejected before any row or audit record is written.
    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    public void service(@RequestBody NewClient body, ClientRepository repository, Audit audit,
            ObjectResponse<Client> response) {
        String email = body.getEmail() == null ? "" : body.getEmail().trim();
        if (!EMAIL.matcher(email).matches()) {
            throw new IllegalArgumentException("a client requires a valid email address");
        }
        // Two clients may not share an email. Reject the duplicate before saving, so no row and no
        // audit record is written. The UNIQUE constraint (V26__client_email_unique.sql) backs this.
        if (repository.existsByEmail(email)) {
            throw new IllegalArgumentException("a client with that email already exists");
        }
        Client client = new Client();
        client.setName(body.getName());
        client.setEmail(email);
        Client saved = repository.save(client);
        audit.record("CLIENT_CREATED id=" + saved.getId() + " name=" + saved.getName());
        response.send(saved);
    }
}
