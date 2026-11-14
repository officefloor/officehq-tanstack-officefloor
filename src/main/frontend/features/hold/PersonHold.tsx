import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useOneAtATime } from '../../query/useOneAtATime';
import { peopleKey } from '../people/people';
import type { Hold, Person } from '../people/people';
import { usePersonError } from '../people/personError';
import { useCurrentSheet, useCurrentUser } from '../session/session';
import { useColumnShown } from '../columns/columns';

const DATE = /^\d{4}-\d{2}-\d{2}$/;

// Whether the person has any hold, each hold (one line apiece), and the dates and button that add another: from a
// date (blank: the sheet's today) to an optional to-date. A hold ends on its to-date; there is no release. Only
// their open claims dated within a hold, and not bound by a statement, count in no live total.
export function PersonHold({ person }: { person: Person }) {
  const showHold = useColumnShown('hold');
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const [, setError] = usePersonError(person.id);
  const queryClient = useQueryClient();
  const once = useOneAtATime();

  const hold = useMutation({
    mutationFn: (dates: Hold) =>
      postJson<Person>(
        `/api/sheets/${encodeURIComponent(sheet)}/people/${person.id}/hold?user=${encodeURIComponent(user)}`,
        dates,
      ),
    onSuccess: async () => {
      setFrom('');
      setTo('');
      await queryClient.invalidateQueries({ queryKey: peopleKey(sheet) });
    },
    onError: () => setError('The person could not be held.'),
  });

  const submit = () => {
    setError('');
    const f = from.trim();
    const t = to.trim();
    if ((f && !DATE.test(f)) || (t && !DATE.test(t))) {
      setError('Type a date as yyyy-mm-dd.');
      return;
    }
    if (f && t && t < f) {
      setError('A hold cannot end before it starts.');
      return;
    }
    once(() => hold.mutateAsync({ from: f || null, to: t || null }));
  };

  return (
    <td>
      {showHold && <span data-testid="person-hold">{person.held ? 'yes' : 'no'}</span>}
      {person.holds.map((h, i) => (
        <div key={i} data-testid="person-hold-line">
          {h.from ?? 'start'} to {h.to ?? 'no end'}
        </div>
      ))}
      <input
        data-testid="person-hold-from"
        placeholder="From yyyy-mm-dd"
        value={from}
        onChange={(e) => {
          setFrom(e.target.value);
          setError('');
        }}
      />
      <input
        data-testid="person-hold-to"
        placeholder="To yyyy-mm-dd (blank: no end)"
        value={to}
        onChange={(e) => {
          setTo(e.target.value);
          setError('');
        }}
      />
      <button type="button" data-testid="person-hold-toggle" disabled={hold.isPending} onClick={submit}>
        Hold
      </button>
    </td>
  );
}
