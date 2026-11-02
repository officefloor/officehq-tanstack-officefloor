import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { byUser } from '../session/session';
import { SheetPanels } from '../../slots/defs/sheetPanels';
import { peopleKey, peoplePath } from './people';
import type { Person } from './people';
import { clearSelectedPeople, useSelectedPeople } from './selection';

type BulkHeld = { held: number; people: Person[] };

// Holds the selected people together from a from-date (blank for the sheet's today) to a to-date (blank for none): the
// server holds each in list order exactly as the row's hold would, and refuses the whole action on a bad date or a
// to-date before the from-date. Shows how many were held.
function BulkHold({ sheet, user }: { sheet: string; user: string }) {
  const queryClient = useQueryClient();
  const selected = useSelectedPeople(sheet);
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const hold = useMutation({
    mutationFn: (body: { people: number[]; from: string; to: string }) =>
      postJson<BulkHeld>(byUser(`${peoplePath(sheet)}/bulk-hold`, user), body),
    onSuccess: (result) => {
      queryClient.setQueryData(peopleKey(sheet), result.people);
      clearSelectedPeople(sheet);
    },
  });
  const act = useSubmitOnce(hold);

  return (
    <p>
      <label>
        Hold from{' '}
        <input
          data-testid="bulk-hold-from"
          placeholder="yyyy-mm-dd"
          value={from}
          onChange={(e) => {
            setFrom(e.target.value);
            hold.reset();
          }}
        />
      </label>{' '}
      <label>
        to{' '}
        <input
          data-testid="bulk-hold-to"
          placeholder="yyyy-mm-dd"
          value={to}
          onChange={(e) => {
            setTo(e.target.value);
            hold.reset();
          }}
        />
      </label>{' '}
      <button
        type="button"
        data-testid="bulk-hold"
        disabled={hold.isPending}
        onClick={() => act({ people: [...selected], from: from.trim(), to: to.trim() })}
      >
        Hold selected
      </button>
      {hold.isSuccess && (
        <>
          {' '}Held <strong data-testid="bulk-held">{hold.data.held}</strong>
        </>
      )}
      {hold.isError && (
        <span data-testid="person-error" role="alert">
          {' '}That bulk hold was refused: a from-date and a to-date that are valid dates or blank, the to-date not
          before the from-date.
        </span>
      )}
    </p>
  );
}

export const contribution = SheetPanels.fill({ order: 12.2, Component: BulkHold });
