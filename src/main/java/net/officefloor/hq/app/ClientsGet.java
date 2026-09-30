package net.officefloor.hq.app;

import java.util.List;
import net.officefloor.web.ObjectResponse;

/**
 * GET /api/clients — every client, oldest id first. Wired by
 * {@code officefloor/rest/api/clients.GET.yml}.
 */
public class ClientsGet {

    public void service(ClientRepository clients, ObjectResponse<List<Client>> response) {
        response.send(clients.findAllByOrderByIdAsc());
    }
}
