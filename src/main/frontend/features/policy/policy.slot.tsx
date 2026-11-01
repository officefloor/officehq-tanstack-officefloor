import { useMutation } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { SheetPanels } from '../../slots/defs/sheetPanels';

/**
 * The sheet's policy versions: one line per version, oldest first,
 * `<version> <from or start> <key>=<value> <count of claims whose policy version it is>` — a claim's
 * policy version is the one in force on its date (DELETED claims left out). It reads the store; it
 * records nothing.
 */
function PolicyVersionsPanel({ sheet }: { user: string; sheet: string }) {
  const show = useMutation({
    mutationFn: () => getJson<string[]>(`/api/sheets/${encodeURIComponent(sheet)}/policy-versions`),
  });

  return (
    <section data-testid="policy-panel">
      <h2>Policy versions</h2>
      <button
        type="button"
        data-testid="policy-show"
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
        <span data-testid="policy-error" role="alert">
          {' '}
          Could not read the policy versions.
        </span>
      )}
      <ol data-testid="policy-list">
        {(show.data ?? []).map((line, i) => (
          <li key={i} data-testid="policy-line">
            {line}
          </li>
        ))}
      </ol>
    </section>
  );
}

export const contribution = SheetPanels.fill({ order: 28.65, Component: PolicyVersionsPanel });
