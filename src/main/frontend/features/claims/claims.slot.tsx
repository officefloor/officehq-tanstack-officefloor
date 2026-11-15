import { SheetBody } from '../../slots/defs/sheetBody';
import { ClaimedTotal } from './ClaimedTotal';
import { TagTotals } from './TagTotals';

export const contributions = [
  SheetBody.fill({ order: 20, Component: ClaimedTotal }),
  SheetBody.fill({ order: 27, Component: TagTotals }),
];
