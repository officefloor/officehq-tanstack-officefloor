import { PersonCells, PersonColumnHead } from '../../slots/defs/personRow';
import { useActive } from './useActive';

/** Whether a person is active: yes when they have an open claim that counts dated within the 30 days
 *  up to and including the sheet's today. */
function PersonActive({ sheetId, personId }: { sheetId: string; personId: number }) {
  const active = useActive(sheetId).get(personId);
  return <td data-testid="person-active">{active === undefined ? '' : active ? 'yes' : 'no'}</td>;
}

export const contributions = [
  PersonColumnHead.fill({ order: 27, Component: () => <th>Active</th> }),
  PersonCells.fill({ order: 27, Component: PersonActive }),
];
