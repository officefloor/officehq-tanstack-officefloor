import { useSyncExternalStore } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { ClaimCells } from '../../slots/defs/claimRow';
import { SheetBody } from '../../slots/defs/sheetBody';
import { useUser } from '../session/useSession';
import { claimsKey } from '../claims/useClaims';

// The claims selected for a bulk action. Kept in a small store rather than the URL so a checkbox
// shows its new state on the same click (a URL update lands a render later). It belongs to one sheet:
// loading another starts with nothing selected.
const NONE: ReadonlySet<number> = new Set();
let selected: { sheetId: string; ids: ReadonlySet<number> } = { sheetId: '', ids: NONE };
const listeners = new Set<() => void>();
const selection = {
  subscribe: (l: () => void) => {
    listeners.add(l);
    return () => {
      listeners.delete(l);
    };
  },
  get: () => selected,
  set: (sheetId: string, ids: ReadonlySet<number>) => {
    selected = { sheetId, ids };
    listeners.forEach((l) => l());
  },
};
function useSelected(sheetId: string): ReadonlySet<number> {
  const current = useSyncExternalStore(selection.subscribe, selection.get);
  return current.sheetId === sheetId ? current.ids : NONE;
}

// A claim's checkbox selecting it for a bulk action.
function ClaimSelect({ sheetId, claim }: { sheetId: string; user: string; personId: number; claim: { id: number } }) {
  const chosen = useSelected(sheetId);
  return (
    <input
      type="checkbox"
      data-testid="claim-select"
      checked={chosen.has(claim.id)}
      onChange={(e) => {
        const next = new Set(chosen);
        if (e.target.checked) next.add(claim.id);
        else next.delete(claim.id);
        selection.set(sheetId, next);
      }}
    />
  );
}

type BulkOpenResult = { opened: number; refused: number };

// Opens the selected claims together; the server numbers them in list order and leaves any it cannot open
// as they are. Shows how many were opened and how many refused; the counts are gone while it runs.
function BulkOpen({ sheetId }: { sheetId: string; user: string }) {
  const chosen = useSelected(sheetId);
  const [user] = useUser();
  const queryClient = useQueryClient();
  const open = useMutation({
    mutationFn: (ids: number[]) => {
      if (ids.length === 0) throw new Error('Select the claims to open');
      return postJson<BulkOpenResult>(`/api/sheets/${encodeURIComponent(sheetId)}/claims/bulk-open`, { ids, by: user ?? '' });
    },
    onSuccess: async () => {
      selection.set(sheetId, NONE);
      await queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) });
    },
  });

  return (
    <section data-testid="bulk-actions">
      <button type="button" data-testid="bulk-open" disabled={open.isPending} onClick={() => open.mutate([...chosen])}>
        Open selected
      </button>{' '}
      {open.isSuccess && (
        <span>
          Opened <strong data-testid="bulk-opened">{open.data.opened}</strong>, refused{' '}
          <strong data-testid="bulk-refused">{open.data.refused}</strong>
        </span>
      )}
      {open.isError && (
        <span data-testid="bulk-open-error" role="alert">
          {open.error.message}
        </span>
      )}
    </section>
  );
}

export const contributions = [
  ClaimCells.fill({ order: 0, Component: ClaimSelect }),
  SheetBody.fill({ order: 15, Component: BulkOpen }),
];
