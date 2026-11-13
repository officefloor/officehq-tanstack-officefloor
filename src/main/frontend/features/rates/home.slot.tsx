import { HomeMain } from '../../slots/defs/homeMain';
import { DueTotal } from './DueTotal';
import { Rates } from './Rates';

export const contributions = [
  HomeMain.fill({ order: 5, Component: Rates }),
  HomeMain.fill({ order: 40, Component: DueTotal }),
];
