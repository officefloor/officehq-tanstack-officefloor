package net.officefloor.hq.app;

import java.util.List;
import net.officefloor.web.ObjectResponse;

/**
 * {@code GET /api/clients} — list every client, oldest first. Wired by
 * {@code officefloor/rest/api/clients.GET.yml}.
 */
public class ClientsGet {

    public void service(ClientRepository repository, ObjectResponse<List<Client>> response) {
        response.send(repository.findAllByOrderByIdAsc());
    }
}
