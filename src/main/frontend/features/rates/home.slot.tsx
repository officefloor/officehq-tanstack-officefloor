import { HomeMain } from '../../slots/defs/homeMain';
import { DueTotal } from './DueTotal';
import { ImportRates } from './ImportRates';
import { Rates } from './Rates';

export const contributions = [
  HomeMain.fill({ order: 5, Component: Rates }),
  HomeMain.fill({ order: 6, Component: ImportRates }),
  HomeMain.fill({ order: 40, Component: DueTotal }),
];
