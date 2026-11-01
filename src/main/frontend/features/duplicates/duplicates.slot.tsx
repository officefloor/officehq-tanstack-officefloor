import { useMutation } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { SheetPanels } from '../../slots/defs/sheetPanels';

/**
 * The sheet's duplicate claims: for each person, date and amount with two or more claims (VOID and
 * DELETED left out), one line `<name> <date> <amount> x<count>`, people in list order, then by date,
 * then by amount. It reads the store; it records nothing.
 */
function DuplicatesPanel({ sheet }: { user: string; sheet: string }) {
  const show = useMutation({
    mutationFn: () => getJson<string[]>(`/api/sheets/${encodeURIComponent(sheet)}/duplicates`),
  });

  return (
    <section data-testid="dups-panel">
      <h2>Duplicate claims</h2>
      <button
        type="button"
        data-testid="dups-show"
        disabled={show.isPending}
        onClick={() => {
          // The old lines go while the new ones are read, so two presses are told apart.
          show.reset();
          show.mutate();
        }}
      >
        Show
      </button>
      {show.isError && (
        <span data-testid="dups-error" role="alert">
          {' '}
          Could not read the duplicate claims.
        </span>
      )}
      <ol data-testid="dups-list">
        {(show.data ?? []).map((line, i) => (
          <li key={i} data-testid="dup-line">
            {line}
          </li>
        ))}
      </ol>
    </section>
  );
}

export const contribution = SheetPanels.fill({ order: 28.6, Component: DuplicatesPanel });
