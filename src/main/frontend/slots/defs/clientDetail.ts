import { defineSlot } from '../Slot';

/**
 * A client's detail region. The client-detail route renders this once; features (its projects, and
 * anything added later) fill it with `ClientDetail.fill(...)`, each panel querying for itself. The
 * context is the client's id, so a contribution can scope its own data to that client.
 */
export const ClientDetail = defineSlot<{ clientId: number }>('client.detail');
