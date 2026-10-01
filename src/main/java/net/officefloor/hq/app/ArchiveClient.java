package net.officefloor.hq.app;

import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * POST /api/clients/archive — tuck a client away rather than delete it, returning the updated row.
 * Wired by officefloor/rest/api/clients/archive.POST.yml.
 *
 * Archiving keeps the client (and everything hanging off it): it only sets the client's archived
 * flag so it drops off the list and the search, which show the not-archived rows. Like deleting, it
 * is an audited side-effect — alongside the change we append one record through the {@link Audit}
 * service ({@code CLIENT_ARCHIVED id=<id>}) so there is a durable note every time a client is tucked
 * away. We reject a missing or unknown client id before writing anything so a bad request neither
 * changes state nor audits.
 */
public class ArchiveClient {

    public void service(@RequestBody ArchiveClientForm form, ClientRepository clients, Audit audit,
            ObjectResponse<ClientView> response) {
        Long id = form.getId();
        if (id == null) {
            throw new IllegalArgumentException("A client id is required");
        }
        Client client = clients.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("A valid client is required"));
        client.archive();
        clients.save(client);
        audit.record("CLIENT_ARCHIVED id=" + id);
        response.send(ClientView.of(client));
    }
}
