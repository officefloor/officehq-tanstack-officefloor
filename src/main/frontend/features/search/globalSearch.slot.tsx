import { HomeContent } from '../../slots/defs/homeContent';
import { GlobalSearch } from './GlobalSearch';

// The global search box's presence on the home page — its own file, filling the shared home.content
// region. The home route is not touched to add it (CLAUDE.md rule 3).
export const contribution = HomeContent.fill({ order: 0, Component: GlobalSearch });
