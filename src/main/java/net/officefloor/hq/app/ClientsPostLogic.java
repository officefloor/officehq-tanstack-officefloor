package net.officefloor.hq.app;

import java.util.regex.Pattern;
import org.springframework.web.bind.annotation.RequestBody;
import net.officefloor.server.http.HttpException;
import net.officefloor.server.http.HttpStatus;
import net.officefloor.web.ObjectResponse;

/**
 * {@code POST /api/clients} — create a client from a {name, email} body and return the saved row
 * (with its generated id). Wired by {@code officefloor/rest/api/clients.POST.yml}. A client is never
 * saved without a proper email address: the email is validated here and rejected with 400 before it
 * reaches the repository (the DB CHECK in Flyway V2 is the matching last line of defence). An email
 * already in use by another client (active or archived) is likewise rejected with 400 before saving,
 * matching the UNIQUE constraint in Flyway V25 — two clients can never share an email.
 */
public class ClientsPostLogic {

    /** A proper email address: local part, @, and a domain with a dot — no whitespace. */
    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    public void service(@RequestBody NewClient newClient, ClientRepository repository,
            ObjectResponse<Client> response) {
        String email = newClient.getEmail();
        if (email == null || !EMAIL.matcher(email.trim()).matches()) {
            throw new HttpException(HttpStatus.BAD_REQUEST, "A proper email address is required");
        }
        String normalised = email.trim();
        if (repository.existsByEmail(normalised)) {
            throw new HttpException(HttpStatus.BAD_REQUEST, "That email is already in use");
        }
        Client saved = repository.save(new Client(newClient.getName(), normalised));
        response.send(saved);
    }

    /** Request body for creating a client. */
    public static class NewClient {
        private String name;
        private String email;

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
