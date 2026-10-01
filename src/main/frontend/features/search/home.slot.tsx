import { HomeMain } from '../../slots/defs/homeMain';
import { GlobalSearch } from './GlobalSearch';

// The global search is the home page's content — it fills the home.main region, so routes/index.tsx
// never lists it. This file is the whole of search's presence on the landing page.
export const contribution = HomeMain.fill({ order: 0, Component: GlobalSearch });
