package net.officefloor.hq.app;

import net.officefloor.server.http.HttpException;
import net.officefloor.server.http.HttpStatus;
import net.officefloor.web.HttpPathParameter;
import net.officefloor.web.ObjectResponse;

/**
 * POST /api/clients/{clientId}/restore — bring a tucked-away client back: clear its archived flag so
 * it returns to the main list and search, and return the updated row. The mirror of
 * {@link ClientArchive}. Wired by {@code officefloor/rest/api/clients/{clientId}/restore.POST.yml}.
 * Restoring is an audited side-effect: one {@code CLIENT_RESTORED id=<id>} record is appended per
 * restore so it can be checked back later (CLAUDE.md — audited behaviour goes through {@link Audit}).
 * An unknown client id is rejected with 404.
 */
public class ClientRestore {

    public void service(@HttpPathParameter("clientId") String clientId,
            ClientRepository clients, Audit audit, ObjectResponse<Client> response) {
        Client client = clients.findById(Long.valueOf(clientId))
                .orElseThrow(() -> new HttpException(HttpStatus.NOT_FOUND));
        client.setArchived(false);
        clients.save(client);
        audit.record("CLIENT_RESTORED id=" + client.getId());
        response.send(client);
    }
}
