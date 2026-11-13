import { AppHeader } from '../../slots/defs/appHeader';
import { SheetPicker } from './SheetPicker';
import { SignIn } from './SignIn';

export const contributions = [
  AppHeader.fill({ order: 0, Component: SignIn }),
  AppHeader.fill({ order: 10, Component: SheetPicker }),
];
