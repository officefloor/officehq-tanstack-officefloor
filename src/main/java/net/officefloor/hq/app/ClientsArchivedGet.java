package net.officefloor.hq.app;

import java.util.List;
import net.officefloor.web.ObjectResponse;

/**
 * GET /api/clients/archived — every client that has been tucked away, oldest id first. The clients
 * list reads this alongside {@code /api/clients} so its "show archived" view can reveal the
 * tucked-away clients and offer to bring one back. Wired by
 * {@code officefloor/rest/api/clients/archived.GET.yml}.
 */
public class ClientsArchivedGet {

    public void service(ClientRepository clients, ObjectResponse<List<Client>> response) {
        response.send(clients.findAllByArchivedTrueOrderByIdAsc());
    }
}
