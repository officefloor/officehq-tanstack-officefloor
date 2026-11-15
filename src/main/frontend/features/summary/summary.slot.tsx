import { SheetBody } from '../../slots/defs/sheetBody';
import { Consolidated } from './Consolidated';
import { Header } from './Header';
import { Summary } from './Summary';

export const contributions = [
  SheetBody.fill({ order: -100, Component: Header }),
  SheetBody.fill({ order: -99, Component: Consolidated }),
  SheetBody.fill({ order: 27, Component: Summary }),
];
