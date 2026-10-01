package net.officefloor.hq.app;

import org.springframework.web.bind.annotation.RequestBody;
import net.officefloor.web.ObjectResponse;

/**
 * {@code POST /api/clients} — create a client from a {name, email} body and return the saved row
 * (with its generated id). Wired by {@code officefloor/rest/api/clients.POST.yml}.
 */
public class ClientsPostLogic {

    public void service(@RequestBody NewClient newClient, ClientRepository repository,
            ObjectResponse<Client> response) {
        Client saved = repository.save(new Client(newClient.getName(), newClient.getEmail()));
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
