package net.officefloor.hq.app;

import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * {@code POST /api/clients} — add a client (name + email) and return the created row. Wired by
 * {@code officefloor/rest/api/clients.POST.yml}. The create is audited through {@link Audit}.
 */
public class ClientsPost {

    public void service(@RequestBody NewClient body, ClientRepository repository, Audit audit,
            ObjectResponse<Client> response) {
        Client client = new Client();
        client.setName(body.getName());
        client.setEmail(body.getEmail());
        Client saved = repository.save(client);
        audit.record("CLIENT_CREATED id=" + saved.getId() + " name=" + saved.getName());
        response.send(saved);
    }
}
