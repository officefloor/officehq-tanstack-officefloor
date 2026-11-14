import { SheetBody } from '../../slots/defs/sheetBody';
import { Roles } from './Roles';
import { Settings } from './Settings';

export const contributions = [
  SheetBody.fill({ order: 6, Component: Settings }),
  SheetBody.fill({ order: 7, Component: Roles }),
];
