import { HomeMain } from '../../slots/defs/homeMain';
import { BulkGrade } from './BulkGrade';
import { PeopleList } from './PeopleList';
import { PersonForm } from './PersonForm';

export const contributions = [
  HomeMain.fill({ order: 10, Component: PersonForm }),
  HomeMain.fill({ order: 20, Component: PeopleList }),
  HomeMain.fill({ order: 24, Component: BulkGrade }),
];
