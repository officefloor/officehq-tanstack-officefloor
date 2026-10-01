package net.officefloor.hq.app;

/** What the API exposes for a client: the shape the front-end renders into the clients table. */
public record ClientView(Long id, String name, String email) {

    public static ClientView of(Client client) {
        return new ClientView(client.getId(), client.getName(), client.getEmail());
    }
}
