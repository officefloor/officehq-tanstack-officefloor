package net.officefloor.hq.app;

import java.util.regex.Pattern;
import org.springframework.web.bind.annotation.RequestBody;
import net.officefloor.server.http.HttpException;
import net.officefloor.server.http.HttpStatus;
import net.officefloor.web.ObjectResponse;

/**
 * {@code POST /api/clients/update} — correct a client's name and email from an {id, name, email}
 * body and return the saved row. Wired by {@code officefloor/rest/api/clients/update.POST.yml}.
 * The client must exist (404 otherwise). The email is held to the same proper-address rule as
 * creation ({@link ClientsPostLogic}) and rejected with 400 before saving (the DB CHECK in Flyway
 * V2 is the matching last line of defence). An email already in use by ANOTHER client (active or
 * archived) is rejected with 400, matching the UNIQUE constraint in Flyway V25 — but keeping a
 * client's own email unchanged is allowed. One {@code CLIENT_UPDATED} audit record is appended so
 * the correction can be checked back later through the audit file.
 */
public class ClientsUpdateLogic {

    /** A proper email address: local part, @, and a domain with a dot — no whitespace. */
    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    public void service(@RequestBody EditClient body, ClientRepository clients, Audit audit,
            ObjectResponse<Client> response) {
        Long id = body.getId();
        if (id == null) {
            throw new HttpException(HttpStatus.BAD_REQUEST, "A client id is required");
        }
        String email = body.getEmail();
        if (email == null || !EMAIL.matcher(email.trim()).matches()) {
            throw new HttpException(HttpStatus.BAD_REQUEST, "A proper email address is required");
        }
        String normalised = email.trim();
        Client client = clients.findById(id)
                .orElseThrow(() -> new HttpException(HttpStatus.NOT_FOUND, "No such client"));
        if (clients.existsByEmailAndIdNot(normalised, id)) {
            throw new HttpException(HttpStatus.BAD_REQUEST, "That email is already in use");
        }
        client.setName(body.getName());
        client.setEmail(normalised);
        Client saved = clients.save(client);
        audit.record("CLIENT_UPDATED id=" + id);
        response.send(saved);
    }

    /** Request body for correcting a client. */
    public static class EditClient {
        private Long id;
        private String name;
        private String email;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }
    }
}
