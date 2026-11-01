import { useState } from 'react';
import type { FormEvent } from 'react';
import { SheetPanels } from '../../slots/defs/sheetPanels';
import { useAuditLines } from './audit';
import type { AuditShown } from './audit';

// The audit viewer: a filter and a show button over the sheet's audit lines, newest first. A blank filter lists every
// line of the sheet; a name lists only the lines about that person (name= or person=, compared as names are). It reads
// the audit file as it stands at each press; it is not a second record. While a press is being read the old lines are
// gone, so the list shown is always the latest press's.
function AuditViewer({ sheet }: { sheet: string; user: string }) {
  const [filter, setFilter] = useState('');
  const [shown, setShown] = useState<AuditShown | null>(null);
  const { data: lines, isSuccess, isFetching, isError } = useAuditLines(sheet, shown);

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setShown((was) => ({ name: filter, press: (was?.press ?? 0) + 1 }));
  };

  return (
    <section>
      <h2>Audit</h2>
      <form onSubmit={submit}>
        <label>
          Person{' '}
          <input
            data-testid="audit-filter"
            placeholder="blank: every line"
            value={filter}
            onChange={(e) => setFilter(e.target.value)}
          />
        </label>{' '}
        <button type="submit" data-testid="audit-show">
          Show
        </button>
        {isError && (
          <span data-testid="audit-error" role="alert">
            {' '}The audit could not be read.
          </span>
        )}
      </form>
      {isSuccess && !isFetching && (
        <ol>
          {lines.map((line, i) => (
            <li key={i} data-testid="audit-line">
              <code>{line}</code>
            </li>
          ))}
        </ol>
      )}
    </section>
  );
}

export const contribution = SheetPanels.fill({ order: 100, Component: AuditViewer });
