import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSubmitOnce } from '../../api/submitOnce';
import { useRowAction } from '../../api/rowAction';
import { PeopleColumns, PersonCells } from '../../slots/defs/peopleColumns';
import { useCurrentSheet, useCurrentUser, useSheetToday } from '../session/session';
import { useClaims, type Claim } from '../claims/claims';
import { formatCents, sumDue } from '../rates/rates';
import { peoplePath, peopleQueryKey, shownName, usePeople, type Person } from './people';
import { useColumnShown } from './columns';

/** How many days back from the sheet's today (today included) a patron's settled claim counts toward backing. */
const BACKING_DAYS = 90;

/** The day a number of days before a date (YYYY-MM-DD). */
const daysBefore = (day: string, n: number) =>
  new Date(Date.parse(`${day}T00:00:00Z`) - n * 86_400_000).toISOString().slice(0, 10);

/** A whole number divided by a positive one, rounded half to even. */
const divideHalfEven = (n: bigint, d: bigint) => {
  const negative = n < 0n;
  const m = negative ? -n : n;
  let q = m / d;
  const rest = (m % d) * 2n;
  if (rest > d || (rest === d && q % 2n === 1n)) q += 1n;
  return negative ? -q : q;
};

/** Each patron's share in hundredths of a percent, in the order they were named: 100.00 split equally, each rounded
 *  half to even to two decimals, the remainder (100.00 minus the shares added up) going to the first-named. */
export function sharesOf(count: number): bigint[] {
  if (count === 0) return [];
  const each = divideHalfEven(10000n, BigInt(count));
  const shares = Array.from({ length: count }, () => each);
  shares[0] += 10000n - each * BigInt(count);
  return shares;
}

/** A patron's figure: their frozen figures over SETTLED claims dated within the window ending today, added up. */
function figureOfPatron(patron: Person, claims: Claim[], today: string, since: string): string {
  return sumDue(claims
    .filter((c) => c.personId === patron.id && c.status === 'SETTLED' && c.due !== null && since <= c.date && c.date <= today)
    .map((c) => c.due as string));
}

/** A person's backing: over their patrons (in the order they were named), each patron's figure times their share
 *  divided by 100, added up exactly and rounded half to even to two decimals. */
export function backingOf(patrons: Person[], claims: Claim[], today: string | undefined): string {
  if (today === undefined) return sumDue([]);
  const since = daysBefore(today, BACKING_DAYS - 1);
  const shares = sharesOf(patrons.length);
  // figure cents times share hundredths-of-a-percent = millionths; dividing by 10000 gives cents.
  const total = patrons.reduce((n, p, i) =>
    n + BigInt(Math.round(Number(figureOfPatron(p, claims, today, since)) * 100)) * shares[i], 0n);
  return formatCents(divideHalfEven(total, 10000n));
}

// A person's patrons — other people on the sheet they have named, one at a time, each once; shown by name,
// each with their share, sorted by name — and the button that names another. Their backing is, over their patrons,
// each patron's figure (their SETTLED claims dated within the 90 days up to and including the sheet's today, each at
// its frozen figure) times their share divided by 100, added up and rounded half to even (0.00 with none).
function PatronsHeader() {
  const backingShown = useColumnShown('backing');
  return (
    <>
      <th>Patrons</th>
      {backingShown && <th>Backing</th>}
    </>
  );
}

function PersonPatrons({ person }: { person: Person }) {
  const patronsShown = useColumnShown('patrons');
  const backingShown = useColumnShown('backing');
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const people = usePeople();
  const claims = useClaims();
  const today = useSheetToday();
  const [chosen, setChosen] = useState('');
  const queryClient = useQueryClient();
  const add = useMutation({
    mutationFn: (body: { patronId: number }) =>
      postJson<Person>(`${peoplePath(sheet ?? '')}/${person.id}/patrons`, body, user),
    onSuccess: () => {
      setChosen('');
      return queryClient.invalidateQueries({ queryKey: peopleQueryKey(sheet ?? '') });
    },
  });
  const submit = useSubmitOnce(add);
  const rowAction = useRowAction(`person-${person.id}`);
  const everyone = people.data ?? [];
  const patrons = person.patronIds
    .map((id) => everyone.find((p) => p.id === id))
    .filter((p): p is Person => p !== undefined);
  const shares = sharesOf(patrons.length);
  const names = patrons.map((p, i) => ({ name: p.name, shown: shownName(p, today), share: formatCents(shares[i]) }))
    .sort((a, b) => (a.name < b.name ? -1 : a.name > b.name ? 1 : 0))
    .map((p) => `${p.shown} ${p.share}`);
  const backing = backingOf(patrons, claims.data ?? [], today);
  return (
    <>
      <td>
        {patronsShown && <><span data-testid="person-patrons">{names.join(', ')}</span>{' '}</>}
        <select
          data-testid="person-patron-select"
          value={chosen}
          onChange={(e) => {
            setChosen(e.target.value);
            add.reset();
          }}
        >
          <option value="">Choose a patron</option>
          {everyone.filter((p) => p.id !== person.id).map((p) => (
            <option key={p.id} value={String(p.id)}>{p.name}</option>
          ))}
        </select>
        <button
          type="button"
          data-testid="person-patron-add"
          disabled={add.isPending}
          onClick={() => { rowAction.take(); submit({ patronId: Number(chosen) }); }}
        >
          Add patron
        </button>
        {add.isError && rowAction.isLatest && <span data-testid="person-patron-error">The patron could not be added.</span>}
      </td>
      {backingShown && <td data-testid="person-backing">{backing}</td>}
    </>
  );
}

export const contributions = [
  PeopleColumns.fill({ order: 47, Component: PatronsHeader }),
  PersonCells.fill({ order: 47, Component: PersonPatrons }),
];
