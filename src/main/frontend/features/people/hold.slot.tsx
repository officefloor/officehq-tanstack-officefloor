import { useState } from 'react';
import { useSubmitOnce } from '../../api/useSubmitOnce';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { PersonCells, PersonColumns } from '../../slots/defs/personRow';
import { useCurrentSheet, useCurrentUser } from '../session/session';
import { holdPerson, peopleKey, usePeople } from './people';

const DATE = /^\d{4}-\d{2}-\d{2}$/;

function HoldColumns() {
  return (
    <>
      <th>On hold</th>
      <th>Hold</th>
    </>
  );
}

// Whether a person has any hold, and their holds; a from-date (empty: the sheet's today), an optional
// to-date and a button that adds a hold. A hold ends on its to-date. A refusal shows on the row.
function PersonHold({ personId }: { personId: number }) {
  const [sheet] = useCurrentSheet();
  const [user] = useCurrentUser();
  const person = (usePeople().data ?? []).find((p) => p.id === personId);
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const [error, setError] = useState('');
  const queryClient = useQueryClient();
  const once = useSubmitOnce();

  const hold = useMutation({
    mutationFn: (dates: { from: string; to: string }) => holdPerson(sheet!, user, personId, dates.from, dates.to),
    onSuccess: async () => {
      setFrom('');
      setTo('');
      await queryClient.invalidateQueries({ queryKey: peopleKey(sheet!) });
    },
    onError: async () => {
      setError('The hold could not be added: check its dates.');
      await queryClient.invalidateQueries({ queryKey: peopleKey(sheet!) });
    },
  });

  const submit = () => {
    setError('');
    if (!person) return;
    const f = from.trim();
    const t = to.trim();
    if ((f !== '' && !DATE.test(f)) || (t !== '' && !DATE.test(t))) {
      setError('The hold dates must be dates (yyyy-mm-dd).');
      return;
    }
    if (f !== '' && t !== '' && t < f) {
      setError('The hold to-date must not be before its from-date.');
      return;
    }
    once(() => hold.mutateAsync({ from: f, to: t }));
  };

  return (
    <>
      <td data-testid="person-hold">{person?.held ? 'yes' : 'no'}</td>
      <td>
        <ul>
          {(person?.holds ?? []).map((h, i) => (
            <li key={i} data-testid="person-hold-line">
              {h.from ?? 'start'} – {h.to ?? 'open'}
            </li>
          ))}
        </ul>
        <input
          type="date"
          data-testid="person-hold-from"
          aria-label="Hold from"
          value={from}
          onChange={(e) => {
            setFrom(e.target.value);
            setError('');
          }}
        />
        <input
          type="date"
          data-testid="person-hold-to"
          aria-label="Hold to"
          value={to}
          onChange={(e) => {
            setTo(e.target.value);
            setError('');
          }}
        />
        <button type="button" data-testid="person-hold-toggle" onClick={submit} disabled={hold.isPending}>
          Hold
        </button>
        {error && <span data-testid="person-error">{error}</span>}
      </td>
    </>
  );
}

export const contributions = [
  PersonColumns.fill({ order: 35, Component: HoldColumns }),
  PersonCells.fill({ order: 35, Component: PersonHold }),
];
