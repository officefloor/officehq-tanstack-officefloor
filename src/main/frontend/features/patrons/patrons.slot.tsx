import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { HttpError, postJson } from '../../api/http';
import { PersonColumnHeads, PersonColumns } from '../../slots/defs/personColumns';
import { type Person, peopleKey, usePeople } from '../people/people';
import { useCurrentSheet, useCurrentUser } from '../../session/session';
import { useSheetToday } from '../sheet/sheet';
import { live, useClaims } from '../claims/claims';
import { useSettings } from '../settings/settings';
import { lateOf } from '../overdue/overdue';
import { dueTotal, personDue, useRates } from '../rates/rates';

function PatronsHead() {
  return <th>Patrons</th>;
}

// The person's patrons (names, sorted) and their backing: the patrons' due-totals added up, skipping
// n/a. Another person on the sheet is named as a patron one at a time; a refusal shows its error until
// the next is tried.
function PersonPatrons({ person }: { person: Person }) {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const people = usePeople(sheet).data ?? [];
  const claims = useClaims(sheet);
  const rates = useRates(sheet);
  const today = useSheetToday(sheet);
  const settings = useSettings(sheet).data;
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

  const patrons = people.filter((p) => person.patronIds.includes(p.id));
  const names = patrons.map((p) => p.name).sort((a, b) => a.localeCompare(b));
  const backing = dueTotal(
    patrons.map((p) =>
      personDue(
        live((claims.data ?? []).filter((c) => c.personId === p.id), [p]),
        rates.data,
        p.grades,
        today,
        settings?.floor,
        lateOf(settings, today),
      ),
    ),
  );
  const candidates = people.filter((p) => p.id !== person.id && !person.patronIds.includes(p.id));

  return (
    <td>
      <span data-testid="person-patrons">{names.join(', ')}</span>{' '}
      <span data-testid="person-backing">{backing}</span>{' '}
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
