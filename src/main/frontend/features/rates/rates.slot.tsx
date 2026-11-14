import { SheetBody } from '../../slots/defs/sheetBody';
import { DueTotal } from './DueTotal';
import { Rates } from './Rates';

export const contributions = [
  SheetBody.fill({ order: 5, Component: Rates }),
  SheetBody.fill({ order: 25, Component: DueTotal }),
];
