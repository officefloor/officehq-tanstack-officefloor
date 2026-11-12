import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { HttpError, postJson } from '../../api/http';
import { PersonColumnHeads, PersonColumns } from '../../slots/defs/personColumns';
import { type Person, peopleKey, shownName, usePeople } from '../people/people';
import { useCurrentSheet, useCurrentUser } from '../../session/session';
import { useSheetToday } from '../sheet/sheet';
import { isRecentSettled, useClaims } from '../claims/claims';
import { NA, dueTotal, patronShares, sharedTotal } from '../rates/rates';
import { useFigure } from '../locale/locale';
import { useColumnShown } from '../columns/columns';

function PatronsHead() {
  const shown = useColumnShown();
  if (!shown('patrons') && !shown('backing')) return null;
  return <th>Patrons</th>;
}

// The person's patrons (each as name and share, sorted by name) and their backing. Shares split 100.00
// equally in the order the patrons were named (see patronShares). A patron's figure is the frozen figures of
// their SETTLED claims dated within the 90 days up to and including the sheet's today, added up; the backing
// is each patron's figure times their share ÷ 100, added up and rounded half to even (0.00 with none).
// Another person on the sheet is named as a patron one at a time; a refusal shows its error until the
// next is tried.
function PersonPatrons({ person }: { person: Person }) {
  const shown = useColumnShown();
  const fig = useFigure();
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const people = usePeople(sheet).data ?? [];
  const claims = useClaims(sheet);
  const today = useSheetToday(sheet);
  const [chosen, setChosen] = useState('');
  const queryClient = useQueryClient();

  const add = useMutation({
    mutationFn: (patronId: number) =>
      postJson<Person>(`/api/sheets/${encodeURIComponent(sheet)}/people/${person.id}/patrons`, { user, patronId }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: peopleKey(sheet) });
      setChosen('');
    },
  });

  const shares = patronShares(person.patronIds.length);
  const patrons = person.patronIds.flatMap((id, i) => {
    const p = people.find((q) => q.id === id);
    return p ? [{ person: p, share: shares[i] }] : [];
  });
  const named = patrons
    .map((p) => ({ person: p.person, share: p.share }))
    .sort((a, b) => a.person.name.localeCompare(b.person.name))
    .map((p) => `${shownName(p.person)} ${p.share}`);
  const backing = sharedTotal(
    patrons.map((p) => ({
      figure: dueTotal(
        (claims.data ?? [])
          .filter((c) => c.personId === p.person.id && isRecentSettled(c, today))
          .map((c) => c.due ?? NA),
      ),
      share: p.share,
    })),
  );
  const candidates = people.filter((p) => p.id !== person.id && !person.patronIds.includes(p.id));

  if (!shown('patrons') && !shown('backing')) return null;
  return (
    <td>
      {shown('patrons') && <><span data-testid="person-patrons">{named.join(', ')}</span>{' '}</>}
      {shown('backing') && <><span data-testid="person-backing">{fig(backing)}</span>{' '}</>}
      <select
        data-testid="person-patron-select"
        value={chosen}
        onChange={(e) => {
          add.reset();
          setChosen(e.target.value);
        }}
      >
        <option value="">Patron…</option>
        {candidates.map((p) => (
          <option key={p.id} value={String(p.id)}>
            {p.name}
          </option>
        ))}
      </select>
      <button
        type="button"
        data-testid="person-patron-add"
        disabled={add.isPending}
        onClick={() => {
          add.reset();
          add.mutate(Number(chosen));
        }}
      >
        Add patron
      </button>
      {add.isError && (
        <span data-testid="person-patron-error" role="alert">
          {' '}
          {add.error instanceof HttpError && add.error.reason ? `${add.error.reason}.` : 'The patron was refused.'}
        </span>
      )}
    </td>
  );
}

export const contributions = [
  PersonColumnHeads.fill({ order: 19.7, Component: PatronsHead }),
  PersonColumns.fill({ order: 19.7, Component: PersonPatrons }),
];
