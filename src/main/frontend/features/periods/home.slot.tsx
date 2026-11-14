import { HomeMain } from '../../slots/defs/homeMain';
import { ImportOpenings } from './ImportOpenings';
import { Periods } from './Periods';

export const contributions = [
  HomeMain.fill({ order: 7, Component: Periods }),
  HomeMain.fill({ order: 7.5, Component: ImportOpenings }),
];
