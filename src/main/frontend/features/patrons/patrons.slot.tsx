import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSubmitOnce } from '../../api/useSubmitOnce';
import { PersonCells, PersonHeaders } from '../../slots/defs/personColumns';
import { peopleQuery, shownName } from '../people/peopleQuery';
import { useSheetToday } from '../sheet/useSheetToday';
import { useUser } from '../session/useUser';
import { patronSharesOf, patronsKey, patronsQuery } from './patronsQuery';
import type { PatronLink } from './patronsQuery';
import { ColumnShown } from '../people/useColumns';

/**
 * The person's patrons (each as name and share, sorted by name, comma separated) and the control that names one more: any other
 * person on the sheet, each at most once.
 */
function PersonPatrons({ sheetId, personId }: { sheetId: string; personId: number }) {
  const queryClient = useQueryClient();
  const [user] = useUser();
  const [patronId, setPatronId] = useState('');
  const [error, setError] = useState('');
  const once = useSubmitOnce();
  const people = useQuery(peopleQuery(sheetId)).data ?? [];
  const links = useQuery(patronsQuery(sheetId)).data ?? [];
  const today = useSheetToday(sheetId);
  const names = patronSharesOf(links, personId)
    .flatMap((s) => {
      const patron = people.find((p) => p.id === s.patronId);
      return patron ? [{ name: patron.name, shown: shownName(patron, today), share: s.share }] : [];
    })
    .sort((a, b) => (a.name < b.name ? -1 : a.name > b.name ? 1 : 0))
    .map((s) => `${s.shown} ${s.share}`);
  const add = useMutation({
    mutationFn: (id: number) =>
      postJson<PatronLink>(`/api/sheets/${encodeURIComponent(sheetId)}/people/${personId}/patrons`, { patronId: id }, user),
    onSuccess: async () => {
      setPatronId('');
      await queryClient.invalidateQueries({ queryKey: patronsKey(sheetId) });
    },
    onError: () => setError('That patron could not be added.'),
  });

  return (
    <td>
      <span data-testid="person-patrons">{names.join(', ')}</span>{' '}
      <select
        data-testid="person-patron-select"
        value={patronId}
        onChange={(e) => {
          setPatronId(e.target.value);
          setError('');
        }}
      >
        <option value="">Patron…</option>
        {people
          .filter((p) => p.id !== personId)
          .map((p) => (
            <option key={p.id} value={String(p.id)}>
              {p.name}
            </option>
          ))}
      </select>{' '}
      <button
        type="button"
        data-testid="person-patron-add"
        disabled={add.isPending}
        onClick={() => {
          setError('');
          if (patronId === '') {
            setError('Choose a patron.');
            return;
          }
          once(() => add.mutateAsync(Number(patronId)));
        }}
      >
        Add patron
      </button>
      {error && <span data-testid="person-patron-error">{error}</span>}
    </td>
  );
}

export const contributions = [
  PersonHeaders.fill({ order: 21, Component: () => <ColumnShown column="patrons"><th>Patrons</th></ColumnShown> }),
  PersonCells.fill({ order: 21, Component: (ctx) => <ColumnShown column="patrons"><PersonPatrons {...ctx} /></ColumnShown> }),
];
