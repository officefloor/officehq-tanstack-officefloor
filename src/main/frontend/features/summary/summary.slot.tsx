import { SheetBody } from '../../slots/defs/sheetBody';
import { Header } from './Header';
import { Summary } from './Summary';

export const contributions = [
  SheetBody.fill({ order: -100, Component: Header }),
  SheetBody.fill({ order: 27, Component: Summary }),
];
