package net.officefloor.hq.app;

import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * POST /api/clients/restore — bring a tucked-away client back, returning the updated row.
 * Wired by officefloor/rest/api/clients/restore.POST.yml.
 *
 * Restoring is the inverse of {@link ArchiveClient}: it only clears the client's archived flag so
 * the client returns to the list and the search (which show the not-archived rows). Like archiving,
 * it is an audited side-effect — alongside the change we append one record through the {@link Audit}
 * service ({@code CLIENT_RESTORED id=<id>}) so there is a durable note every time a client is
 * brought back. We reject a missing or unknown client id before writing anything so a bad request
 * neither changes state nor audits.
 */
public class RestoreClient {

    public void service(@RequestBody RestoreClientForm form, ClientRepository clients, Audit audit,
            ObjectResponse<ClientView> response) {
        Long id = form.getId();
        if (id == null) {
            throw new IllegalArgumentException("A client id is required");
        }
        Client client = clients.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("A valid client is required"));
        client.restore();
        clients.save(client);
        audit.record("CLIENT_RESTORED id=" + id);
        response.send(ClientView.of(client));
    }
}
