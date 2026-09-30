package net.officefloor.hq.app;

import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * POST /api/clients — create a client from {name, email} and return the saved row (with its id).
 * Wired by {@code officefloor/rest/api/clients.POST.yml}.
 */
public class ClientsPost {

    public void service(@RequestBody NewClient body, ClientRepository clients,
            ObjectResponse<Client> response) {
        Client client = new Client();
        client.setName(body.getName());
        client.setEmail(body.getEmail());
        response.send(clients.save(client));
    }
}
