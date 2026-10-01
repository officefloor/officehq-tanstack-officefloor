import { defineSlot } from '../Slot';

/**
 * A client's detail page. Features fill this region with panels about one client (the projects it
 * owns, and whatever else later joins by the client id). The detail route renders it with the
 * client id as context; the page never lists what goes in it, so each panel is a new *.slot.tsx.
 */
export const ClientDetail = defineSlot<{ clientId: number }>('client.detail');
