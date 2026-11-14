import { HomeMain } from '../../slots/defs/homeMain';
import { ImportOpenings } from './ImportOpenings';
import { Periods } from './Periods';
import { Rollover } from './Rollover';

export const contributions = [
  HomeMain.fill({ order: 7, Component: Periods }),
  HomeMain.fill({ order: 7.5, Component: ImportOpenings }),
  HomeMain.fill({ order: 7.6, Component: Rollover }),
];
