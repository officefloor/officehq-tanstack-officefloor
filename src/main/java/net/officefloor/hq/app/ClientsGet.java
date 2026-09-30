package net.officefloor.hq.app;

import java.util.List;
import net.officefloor.web.ObjectResponse;

/**
 * GET /api/clients — every client that has not been tucked away, oldest id first. Archived clients
 * are excluded so they drop off the list and search. Wired by
 * {@code officefloor/rest/api/clients.GET.yml}.
 */
public class ClientsGet {

    public void service(ClientRepository clients, ObjectResponse<List<Client>> response) {
        response.send(clients.findAllByArchivedFalseOrderByIdAsc());
    }
}
