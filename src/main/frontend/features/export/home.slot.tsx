import { HomeMain } from '../../slots/defs/homeMain';
import { ExportClaims } from './ExportClaims';
import { ExportPeople } from './ExportPeople';
import { ExportPeriods } from './ExportPeriods';
import { ExportPolicy } from './ExportPolicy';
import { ExportPools } from './ExportPools';
import { ExportTree } from './ExportTree';

export const contributions = [
  HomeMain.fill({ order: 21, Component: ExportPeople }),
  HomeMain.fill({ order: 21.1, Component: ExportClaims }),
  HomeMain.fill({ order: 21.2, Component: ExportPools }),
  HomeMain.fill({ order: 21.3, Component: ExportPeriods }),
  HomeMain.fill({ order: 21.4, Component: ExportTree }),
  HomeMain.fill({ order: 21.45, Component: ExportPolicy }),
];
