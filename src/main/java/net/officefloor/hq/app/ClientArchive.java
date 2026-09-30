package net.officefloor.hq.app;

import net.officefloor.server.http.HttpException;
import net.officefloor.server.http.HttpStatus;
import net.officefloor.web.HttpPathParameter;
import net.officefloor.web.ObjectResponse;

/**
 * POST /api/clients/{clientId}/archive — tuck a client away instead of deleting it: mark it archived
 * so it drops off the list and search but nothing is lost, and return the updated row. Wired by
 * {@code officefloor/rest/api/clients/{clientId}/archive.POST.yml}. Archiving is an audited
 * side-effect: one {@code CLIENT_ARCHIVED id=<id>} record is appended per archive so it can be
 * checked back later (CLAUDE.md — audited behaviour goes through {@link Audit}). An unknown client id
 * is rejected with 404.
 */
public class ClientArchive {

    public void service(@HttpPathParameter("clientId") String clientId,
            ClientRepository clients, Audit audit, ObjectResponse<Client> response) {
        Client client = clients.findById(Long.valueOf(clientId))
                .orElseThrow(() -> new HttpException(HttpStatus.NOT_FOUND));
        client.setArchived(true);
        clients.save(client);
        audit.record("CLIENT_ARCHIVED id=" + client.getId());
        response.send(client);
    }
}
