import { AppHeader } from '../../slots/defs/appHeader';
import { SheetPicker } from './SheetPicker';
import { SheetToday } from './SheetToday';
import { SignIn } from './SignIn';

export const contributions = [
  AppHeader.fill({ order: 0, Component: SignIn }),
  AppHeader.fill({ order: 10, Component: SheetPicker }),
  AppHeader.fill({ order: 20, Component: SheetToday }),
];
