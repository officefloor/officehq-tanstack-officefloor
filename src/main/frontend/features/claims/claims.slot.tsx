import { SheetBody } from '../../slots/defs/sheetBody';
import { ClaimedTotal } from './ClaimedTotal';
import { Duplicates } from './Duplicates';
import { TagTotals } from './TagTotals';
import { VoidReasons } from './VoidReasons';

export const contributions = [
  SheetBody.fill({ order: 20, Component: ClaimedTotal }),
  SheetBody.fill({ order: 27, Component: TagTotals }),
  SheetBody.fill({ order: 27.5, Component: Duplicates }),
  SheetBody.fill({ order: 9.5, Component: VoidReasons }),
];
