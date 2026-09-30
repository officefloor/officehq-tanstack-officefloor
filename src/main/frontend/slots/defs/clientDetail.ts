import { defineSlot } from '../Slot';

/**
 * The body of a client's detail page — the region where per-client panels live (contacts, and
 * anything added later). Declared once here; features fill it without the page listing what goes in
 * it (CLAUDE.md rule 3). Its context is the client's id.
 */
export const ClientDetail = defineSlot<{ clientId: number }>('client.detail');
