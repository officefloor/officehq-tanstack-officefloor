import { HomeMain } from '../../slots/defs/homeMain';
import { Roles } from './Roles';
import { SettingsForm } from './SettingsForm';

export const contributions = [
  HomeMain.fill({ order: 8, Component: SettingsForm }),
  HomeMain.fill({ order: 9, Component: Roles }),
];
