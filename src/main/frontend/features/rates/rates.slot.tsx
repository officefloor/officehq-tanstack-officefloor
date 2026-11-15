import { SheetBody } from '../../slots/defs/sheetBody';
import { DueTotal } from './DueTotal';
import { MonthTotals } from './MonthTotals';
import { Overdue } from './Overdue';
import { Rates } from './Rates';

export const contributions = [
  SheetBody.fill({ order: 5, Component: Rates }),
  SheetBody.fill({ order: 25, Component: DueTotal }),
  SheetBody.fill({ order: 26, Component: Overdue }),
  SheetBody.fill({ order: 26.5, Component: MonthTotals }),
];
