import { HomeMain } from '../../slots/defs/homeMain';
import { BulkOpen } from './BulkOpen';
import { ClaimedTotal } from './ClaimedTotal';

export const contributions = [
  HomeMain.fill({ order: 25, Component: BulkOpen }),
  HomeMain.fill({ order: 30, Component: ClaimedTotal }),
];
