import { defineSlot } from '../Slot';

/**
 * The Clients page toolbar — a region above the list for controls that act on it (search, and
 * whatever comes later). The page renders the region; features fill it (CLAUDE.md rule 3), so a new
 * control is a new `*.slot.tsx` file and the page is never edited again.
 */
export const ClientsToolbar = defineSlot('clients.toolbar');
