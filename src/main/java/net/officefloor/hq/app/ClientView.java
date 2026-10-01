package net.officefloor.hq.app;

/**
 * What the API exposes for a client: the shape the front-end renders into the clients table.
 * Carries the {@code archived} flag so the list can keep tucked-away clients hidden by default and
 * reveal (and restore) them when the "show archived" toggle is on.
 */
public record ClientView(Long id, String name, String email, boolean archived, String currency) {

    public static ClientView of(Client client) {
        return new ClientView(client.getId(), client.getName(), client.getEmail(),
                client.isArchived(), client.getCurrency());
    }
}
