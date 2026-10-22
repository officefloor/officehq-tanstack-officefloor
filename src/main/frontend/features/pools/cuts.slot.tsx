import { PersonRow } from '../../slots/defs/personRow';
import { SheetBody } from '../../slots/defs/sheet';
import { usePeople } from '../people/people';
import type { Person } from '../people/people';
import { personCutTotal, sheetCutTotal, usePools } from './pools';

/** The person's cut total: their cuts across every pool they are a member of, added up. */
function PersonCutTotal({ sheetId, person }: { sheetId: string; user: string; person: Person }) {
  const pools = usePools(sheetId).data ?? [];
  return <td data-testid="person-cut-total">{personCutTotal(pools, person.id)}</td>;
}

/** The sheet's cut total: everyone's cut total added up. */
function CutTotal({ sheetId }: { sheetId: string; user: string }) {
  const pools = usePools(sheetId).data ?? [];
  const people = usePeople(sheetId).data ?? [];
  return (
    <p>
      Cut total <strong data-testid="rollup-cut-total">{sheetCutTotal(pools, people.map((p) => p.id))}</strong>
    </p>
  );
}

export const contributions = [
  SheetBody.fill({ order: 21.8, Component: CutTotal }),
  PersonRow.fill({ order: 27, Component: PersonCutTotal }),
];
