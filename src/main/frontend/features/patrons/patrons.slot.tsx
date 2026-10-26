import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { PersonCells, PersonHeads } from '../../slots/defs/personRow';
import { peopleKey, usePeople, type Person } from '../people/people.slot';
import { personActionKey, useLastPersonAction } from '../people/personAction';
import { addDays, showHundredths, toHundredths } from '../rates/useRates';
import { useClaims } from '../claims/useClaims';
import { useSheetToday } from '../today/today.slot';
import { useUser } from '../session/useSession';
import { useColumnShown } from '../columns/useColumns';

/** n divided by d (d > 0), rounded half to even. */
function roundHalfEven(n: bigint, d: bigint): bigint {
  let q = n / d;
  let r = n % d;
  if (r < 0n) {
    q -= 1n;
    r += d;
  }
  if (r * 2n > d || (r * 2n === d && q % 2n !== 0n)) q += 1n;
  return q;
}

function PatronHeads({ sheetId }: { sheetId: string; user: string }) {
  const shown = useColumnShown(sheetId);
  return (
    <>
      {shown('patrons') && <th>Patrons</th>}
      {shown('backing') && <th>Backing</th>}
      <th />
    </>
  );
}

// The person's patrons (each as name and share, sorted by name), their backing — over the patrons, each patron's figure
// (their SETTLED claims dated within the 90 days up to and including the sheet's today, each at its frozen figure,
// added up) times their share divided by 100, added up and rounded half to even (0.00 with none) — and the picker that names one more. Only other people not yet their patrons are offered. A refusal shows on
// the person until the picker is changed or the next action on their row.
function PatronCells({ sheetId, personId }: { sheetId: string; user: string; personId: number }) {
  const shown = useColumnShown(sheetId);
  const people = usePeople(sheetId).data ?? [];
  const person = people.find((p) => p.id === personId);
  const claims = useClaims(sheetId).data ?? [];
  const today = useSheetToday(sheetId).data?.today;
  const [chosen, setChosen] = useState('');
  const [user] = useUser();
  const queryClient = useQueryClient();
  const add = useMutation({
    mutationKey: personActionKey(sheetId, personId),
    mutationFn: async (patron: string) => {
      if (patron === '') throw new Error('Choose a patron');
      return postJson<Person>(`/api/sheets/${encodeURIComponent(sheetId)}/people/${personId}/patrons`, {
        patron: Number(patron),
        by: user ?? '',
      });
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: peopleKey(sheetId) });
      setChosen('');
    },
  });
  const last = useLastPersonAction(sheetId, personId, add.submittedAt);
  const patrons = person?.patrons ?? [];
  const offered = people.filter((p) => p.id !== personId && !patrons.includes(p.name));
  const shares = person?.patronShares ?? [];
  let backing = 0n;
  if (today !== undefined) {
    const since = addDays(today, -89);
    // Each patron's share in hundredths of a percent, by their id.
    const shareOf = new Map<number, bigint>();
    for (const s of shares) {
      const patron = people.find((p) => p.name === s.name);
      if (patron) shareOf.set(patron.id, toHundredths(s.share));
    }
    // Cents times hundredths of a percent: ten-thousandths of a cent.
    let weighted = 0n;
    for (const c of claims) {
      const share = shareOf.get(c.personId);
      if (share === undefined || c.status !== 'SETTLED' || c.settledDue == null) continue;
      if (c.date < since || c.date > today) continue;
      weighted += toHundredths(c.settledDue) * share;
    }
    backing = roundHalfEven(weighted, 10_000n);
  }

  return (
    <>
      {shown('patrons') && <td data-testid="person-patrons">{shares.map((s) => `${s.name} ${s.share}`).join(', ')}</td>}
      {shown('backing') && <td data-testid="person-backing">{showHundredths(backing)}</td>}
      <td>
        <select
          data-testid="person-patron-select"
          value={chosen}
          onChange={(e) => {
            setChosen(e.target.value);
            add.reset();
          }}
        >
          <option value="">Patron…</option>
          {offered.map((p) => (
            <option key={p.id} value={String(p.id)}>
              {p.name}
            </option>
          ))}
        </select>{' '}
        <button type="button" data-testid="person-patron-add" disabled={add.isPending} onClick={() => add.mutate(chosen)}>
          Add patron
        </button>
        {add.isError && last && (
          <span data-testid="person-patron-error" role="alert">
            {add.error.message}
          </span>
        )}
      </td>
    </>
  );
}

export const contributions = [
  PersonHeads.fill({ order: 50, Component: PatronHeads }),
  PersonCells.fill({ order: 50, Component: PatronCells }),
];
