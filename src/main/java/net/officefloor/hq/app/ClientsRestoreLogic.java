package net.officefloor.hq.app;

import org.springframework.web.bind.annotation.RequestBody;
import net.officefloor.server.http.HttpException;
import net.officefloor.server.http.HttpStatus;
import net.officefloor.web.ObjectResponse;

/**
 * {@code POST /api/clients/restore} — bring a tucked-away client back, from an {id} body, and return
 * the id that was restored. Wired by {@code officefloor/rest/api/clients/restore.POST.yml}. The
 * mirror of {@link ClientsArchiveLogic}: the client must exist (404 otherwise), its {@code archived}
 * flag is cleared so it returns to the list and search, and one {@code CLIENT_RESTORED} audit record
 * is appended so the restore can be checked back later through the audit file.
 */
public class ClientsRestoreLogic {

    public void service(@RequestBody RestoreClient body, ClientRepository clients, Audit audit,
            ObjectResponse<RestoredClient> response) {
        Long id = body.getId();
        if (id == null) {
            throw new HttpException(HttpStatus.BAD_REQUEST, "A client id is required");
        }
        Client client = clients.findById(id)
                .orElseThrow(() -> new HttpException(HttpStatus.NOT_FOUND, "No such client"));
        client.setArchived(false);
        clients.save(client);
        audit.record("CLIENT_RESTORED id=" + id);
        response.send(new RestoredClient(id));
    }

    /** Request body for restoring a client. */
    public static class RestoreClient {
        private Long id;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }
    }

    /** The id of the client that was restored. */
    public static class RestoredClient {
        private final Long id;

        public RestoredClient(Long id) {
            this.id = id;
        }

        public Long getId() {
            return id;
        }
    }
}
