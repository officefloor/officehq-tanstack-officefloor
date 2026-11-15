import { SheetBody } from '../../slots/defs/sheetBody';
import { Classic } from './Classic';
import { Consolidated } from './Consolidated';
import { Header } from './Header';
import { Summary } from './Summary';
import { TreeSearch } from './TreeSearch';

export const contributions = [
  SheetBody.fill({ order: -100, Component: Header }),
  SheetBody.fill({ order: -99, Component: Consolidated }),
  SheetBody.fill({ order: -98, Component: TreeSearch }),
  SheetBody.fill({ order: 27, Component: Summary }),
  SheetBody.fill({ order: 28, Component: Classic }),
];
