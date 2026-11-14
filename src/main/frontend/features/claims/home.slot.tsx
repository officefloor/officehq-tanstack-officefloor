import { HomeMain } from '../../slots/defs/homeMain';
import { BulkOpen } from './BulkOpen';
import { BulkReassign } from './BulkReassign';
import { BulkRedate } from './BulkRedate';
import { BulkSettle } from './BulkSettle';
import { BulkVoid } from './BulkVoid';
import { ClaimedTotal } from './ClaimedTotal';

export const contributions = [
  HomeMain.fill({ order: 25, Component: BulkOpen }),
  HomeMain.fill({ order: 26, Component: BulkSettle }),
  HomeMain.fill({ order: 27, Component: BulkReassign }),
  HomeMain.fill({ order: 28, Component: BulkRedate }),
  HomeMain.fill({ order: 29, Component: BulkVoid }),
  HomeMain.fill({ order: 30, Component: ClaimedTotal }),
];
