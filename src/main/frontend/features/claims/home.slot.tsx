import { HomeMain } from '../../slots/defs/homeMain';
import { BulkOpen } from './BulkOpen';
import { BulkReassign } from './BulkReassign';
import { BulkSettle } from './BulkSettle';
import { ClaimedTotal } from './ClaimedTotal';

export const contributions = [
  HomeMain.fill({ order: 25, Component: BulkOpen }),
  HomeMain.fill({ order: 26, Component: BulkSettle }),
  HomeMain.fill({ order: 27, Component: BulkReassign }),
  HomeMain.fill({ order: 30, Component: ClaimedTotal }),
];
