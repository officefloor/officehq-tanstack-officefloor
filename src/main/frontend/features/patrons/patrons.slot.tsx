import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSubmitOnce } from '../../api/useSubmitOnce';
import { PersonCells, PersonHeaders } from '../../slots/defs/personColumns';
import { peopleQuery } from '../people/peopleQuery';
import { useUser } from '../session/useUser';
import { patronIdsOf, patronsKey, patronsQuery } from './patronsQuery';
import type { PatronLink } from './patronsQuery';

/**
 * The person's patrons (names, sorted, comma separated) and the control that names one more: any other
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
  const ids = patronIdsOf(links, personId);
  const names = people
    .filter((p) => ids.includes(p.id))
    .map((p) => p.name)
    .sort((a, b) => (a < b ? -1 : a > b ? 1 : 0));
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
  PersonHeaders.fill({ order: 21, Component: () => <th>Patrons</th> }),
  PersonCells.fill({ order: 21, Component: PersonPatrons }),
];
