package net.officefloor.hq.app;

import org.springframework.web.bind.annotation.RequestBody;
import net.officefloor.server.http.HttpException;
import net.officefloor.server.http.HttpStatus;
import net.officefloor.web.ObjectResponse;

/**
 * {@code POST /api/clients/archive} — tuck a client away instead of deleting it, from an {id} body,
 * and return the id that was archived. Wired by {@code officefloor/rest/api/clients/archive.POST.yml}.
 * The client must exist (404 otherwise). The client is kept (nothing is lost): its {@code archived}
 * flag is set so it drops off the list and search but the row is retained. One
 * {@code CLIENT_ARCHIVED} audit record is appended so the archiving can be checked back later through
 * the audit file even though the UI only drops the row.
 */
public class ClientsArchiveLogic {

    public void service(@RequestBody ArchiveClient body, ClientRepository clients, Audit audit,
            ObjectResponse<ArchivedClient> response) {
        Long id = body.getId();
        if (id == null) {
            throw new HttpException(HttpStatus.BAD_REQUEST, "A client id is required");
        }
        Client client = clients.findById(id)
                .orElseThrow(() -> new HttpException(HttpStatus.NOT_FOUND, "No such client"));
        client.setArchived(true);
        clients.save(client);
        audit.record("CLIENT_ARCHIVED id=" + id);
        response.send(new ArchivedClient(id));
    }

    /** Request body for archiving a client. */
    public static class ArchiveClient {
        private Long id;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }
    }

    /** The id of the client that was archived. */
    public static class ArchivedClient {
        private final Long id;

        public ArchivedClient(Long id) {
            this.id = id;
        }

        public Long getId() {
            return id;
        }
    }
}
