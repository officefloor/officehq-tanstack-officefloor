import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { byUser } from '../session/session';
import { SheetPanels } from '../../slots/defs/sheetPanels';
import { peopleKey, peoplePath } from './people';
import type { Person } from './people';
import { clearSelectedPeople, useSelectedPeople } from './selection';

type BulkRetired = { retired: number; people: Person[] };

// Retires the selected people together from a date (blank for the sheet's today): the server retires each in list
// order exactly as the row's retire would, and refuses the whole action on a bad date. Shows how many were retired.
function BulkRetire({ sheet, user }: { sheet: string; user: string }) {
  const queryClient = useQueryClient();
  const selected = useSelectedPeople(sheet);
  const [from, setFrom] = useState('');
  const retire = useMutation({
    mutationFn: (body: { people: number[]; from: string }) =>
      postJson<BulkRetired>(byUser(`${peoplePath(sheet)}/bulk-retire`, user), body),
    onSuccess: (result) => {
      queryClient.setQueryData(peopleKey(sheet), result.people);
      clearSelectedPeople(sheet);
    },
  });
  const act = useSubmitOnce(retire);

  return (
    <p>
      <label>
        Retire from{' '}
        <input
          data-testid="bulk-retire-date"
          placeholder="yyyy-mm-dd"
          value={from}
          onChange={(e) => {
            setFrom(e.target.value);
            retire.reset();
          }}
        />
      </label>{' '}
      <button
        type="button"
        data-testid="bulk-retire"
        disabled={retire.isPending}
        onClick={() => act({ people: [...selected], from: from.trim() })}
      >
        Retire selected
      </button>
      {retire.isSuccess && (
        <>
          {' '}Retired <strong data-testid="bulk-retired">{retire.data.retired}</strong>
        </>
      )}
      {retire.isError && (
        <span data-testid="person-error" role="alert">
          {' '}That bulk retire was refused: a from-date that is a valid date or blank.
        </span>
      )}
    </p>
  );
}

export const contribution = SheetPanels.fill({ order: 12.1, Component: BulkRetire });
