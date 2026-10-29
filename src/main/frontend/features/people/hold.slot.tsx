import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { putJson } from '../../api/http';
import { useSubmitOnce } from '../../api/useSubmitOnce';
import { PersonCells, PersonHeaders } from '../../slots/defs/personColumns';
import { usePersonError } from './usePersonError';
import { useUser } from '../session/useUser';
import { peopleKey, peopleQuery } from './peopleQuery';
import type { Person } from './peopleQuery';
import { ColumnShown } from './useColumns';

const DATE = /^\d{4}-\d{2}-\d{2}$/;

/**
 * Whether the person has any hold, their holds, and the control that adds one from a from-date (blank: the
 * sheet's today) to an optional to-date. Their OPEN claims dated within a hold, unless bound by a statement,
 * count in no live total; a hold ends on its to-date.
 */
function PersonHold({ sheetId, personId }: { sheetId: string; personId: number }) {
  const queryClient = useQueryClient();
  const [user] = useUser();
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const [error, setError] = usePersonError(personId);
  const once = useSubmitOnce();
  const person = (useQuery(peopleQuery(sheetId)).data ?? []).find((p) => p.id === personId);
  const hold = useMutation({
    mutationFn: (body: { from: string; to: string }) =>
      putJson<Person>(`/api/sheets/${encodeURIComponent(sheetId)}/people/${personId}/hold`, body, user),
    onSuccess: async () => {
      setFrom('');
      setTo('');
      await queryClient.invalidateQueries({ queryKey: peopleKey(sheetId) });
    },
    onError: () => setError('The hold could not be added.'),
  });

  return (
    <td>
      <span data-testid="person-hold">{person?.held ? 'yes' : 'no'}</span>{' '}
      {(person?.holds ?? []).map((h, i) => (
        <span key={i} data-testid="person-hold-line">
          {h.from} to {h.to ?? '…'}{' '}
        </span>
      ))}
      <input
        data-testid="person-hold-from"
        placeholder="from yyyy-mm-dd"
        value={from}
        onChange={(e) => {
          setFrom(e.target.value);
          setError('');
        }}
      />{' '}
      <input
        data-testid="person-hold-to"
        placeholder="to yyyy-mm-dd"
        value={to}
        onChange={(e) => {
          setTo(e.target.value);
          setError('');
        }}
      />{' '}
      <button
        type="button"
        data-testid="person-hold-toggle"
        disabled={hold.isPending}
        onClick={() => {
          setError('');
          const f = from.trim();
          const t = to.trim();
          if ((f !== '' && !DATE.test(f)) || (t !== '' && !DATE.test(t))) {
            setError('The dates must be yyyy-mm-dd.');
            return;
          }
          if (f !== '' && t !== '' && t < f) {
            setError('The to-date is before the from-date.');
            return;
          }
          once(() => hold.mutateAsync({ from: f, to: t }));
        }}
      >
        Hold
      </button>
      {error && <span data-testid="person-error">{error}</span>}
    </td>
  );
}

export const contributions = [
  PersonHeaders.fill({ order: 19, Component: () => <ColumnShown column="hold"><th>Hold</th></ColumnShown> }),
  PersonCells.fill({ order: 19, Component: (ctx) => <ColumnShown column="hold"><PersonHold {...ctx} /></ColumnShown> }),
];
