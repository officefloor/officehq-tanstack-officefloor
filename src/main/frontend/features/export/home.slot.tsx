import { HomeMain } from '../../slots/defs/homeMain';
import { ExportClaims } from './ExportClaims';
import { ExportPeople } from './ExportPeople';
import { ExportPools } from './ExportPools';

export const contributions = [
  HomeMain.fill({ order: 21, Component: ExportPeople }),
  HomeMain.fill({ order: 21.1, Component: ExportClaims }),
  HomeMain.fill({ order: 21.2, Component: ExportPools }),
];
