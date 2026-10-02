import { defineSlot } from '../Slot';

/**
 * The home page's body — the landing region features fill. The home route renders this region and
 * never lists what goes in it; each thing that lives on home (the global search box, and more over
 * time) is its own `*.slot.tsx` file under `features/`.
 */
export const HomeContent = defineSlot('home.content');
