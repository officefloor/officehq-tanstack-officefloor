package net.officefloor.hq.app;

import java.util.List;
import net.officefloor.web.ObjectResponse;

/**
 * {@code GET /api/clients} — list every client. Wired by
 * {@code officefloor/rest/api/clients.GET.yml}.
 */
public class ClientsGetLogic {

    public void service(ClientRepository repository, ObjectResponse<List<Client>> response) {
        response.send(repository.findAll());
    }
}
