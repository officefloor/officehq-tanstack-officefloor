import { defineSlot } from '../Slot';

/**
 * A client's detail page — panels about one client (its contacts, and more over time). The detail
 * route renders this region and never lists what fills it; each panel is its own `*.slot.tsx` file
 * under `features/clients/`. Handed the client id as context.
 */
export const ClientDetail = defineSlot<{ clientId: number }>('client.detail');
