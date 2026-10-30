import { createContext, useContext, useState, type ReactNode } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { asString, useSearchParam } from '../../url/useSearchParam';
import { claimsKey } from './useClaims';

type Selection = { selected: Set<string>; toggle: (id: string, on: boolean) => void; clear: () => void };

const SelectionContext = createContext<Selection | null>(null);

/** The claims ticked with their select checkbox, shared by every claim row and the bulk buttons. */
export function ClaimSelection({ children }: { children: ReactNode }) {
  const [selected, setSelected] = useState<Set<string>>(() => new Set());
  const toggle = (id: string, on: boolean) =>
    setSelected((was) => {
      const next = new Set(was);
      if (on) next.add(id);
      else next.delete(id);
      return next;
    });
  const clear = () => setSelected(new Set());
  return <SelectionContext.Provider value={{ selected, toggle, clear }}>{children}</SelectionContext.Provider>;
}

export function useClaimSelection(): Selection {
  const selection = useContext(SelectionContext);
  if (!selection) throw new Error('useClaimSelection outside ClaimSelection');
  return selection;
}

/** A claim row's select checkbox. */
export function ClaimSelect({ claimId }: { claimId: string }) {
  const { selected, toggle } = useClaimSelection();
  return (
    <input
      type="checkbox"
      data-testid="claim-select"
      checked={selected.has(claimId)}
      onChange={(e) => toggle(claimId, e.target.checked)}
    />
  );
}

// Opens the selected claims together: the server opens and numbers each that can be opened, in list
// order (people in the order they were added, newest claim first within a person), and leaves each
// that cannot as it is. Then shows how many were opened and how many were refused, and clears the
// selection; pressing again removes the old counts until the new ones are in.
export function BulkOpenButton() {
  const [sheet] = useSearchParam('sheet', asString);
  const [user] = useSearchParam('user', asString);
  const queryClient = useQueryClient();
  const once = useSubmitOnce();
  const { selected, clear } = useClaimSelection();

  const open = useMutation({
    mutationFn: (claimIds: string[]) =>
      postJson<{ opened: number; refused: number }>(
        `/api/sheets/${encodeURIComponent(sheet)}/claims/bulk-open`,
        { claimIds, user },
      ),
    onSuccess: () => {
      clear();
      return queryClient.invalidateQueries({ queryKey: claimsKey(sheet) });
    },
  });

  return (
    <p>
      <button
        type="button"
        data-testid="bulk-open"
        onClick={() => {
          open.reset();
          const ids = [...selected];
          once(() => open.mutateAsync(ids));
        }}
      >
        Open selected
      </button>
      {open.isSuccess && (
        <>
          {' '}
          Opened <span data-testid="bulk-opened">{open.data.opened}</span>, refused{' '}
          <span data-testid="bulk-refused">{open.data.refused}</span>
        </>
      )}
      {open.isError && (
        <span data-testid="bulk-open-error" role="alert">
          {' '}
          The selected claims could not be opened.
        </span>
      )}
    </p>
  );
}
