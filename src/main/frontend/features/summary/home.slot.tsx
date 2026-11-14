import { HomeMain } from '../../slots/defs/homeMain';
import { ClassicSummary } from './ClassicSummary';
import { Summary } from './Summary';

export const contributions = [
  HomeMain.fill({ order: 50, Component: Summary }),
  HomeMain.fill({ order: 51, Component: ClassicSummary }),
];
