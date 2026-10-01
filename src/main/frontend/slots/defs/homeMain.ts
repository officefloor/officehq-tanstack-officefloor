import { defineSlot } from '../Slot';

/**
 * The home page's main region. Features fill it with whatever belongs on the landing page; the page
 * never lists what goes in it, so each addition is a new *.slot.tsx. Rendered by routes/index.tsx.
 */
export const HomeMain = defineSlot('home.main');
