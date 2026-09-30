package net.officefloor.hq.app;

/**
 * Request body for {@code POST /api/clients/restore}: the id of the archived client to bring back.
 * Bound from the JSON payload by the Spring MVC {@code @RequestBody} argument resolver.
 */
public class RestoreClient {

    private Long id;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
