import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { SheetBody } from '../../slots/defs/sheet';

/** The audit viewer: the sheet's audit lines exactly as the audit file holds them, newest first. A name in the
 *  filter shows only the lines about that person (name= or person= that name, compared as names are). Each press
 *  of Show re-reads the file. */
function AuditViewer({ sheetId }: { sheetId: string; user: string }) {
  const [filter, setFilter] = useState('');
  const [shown, setShown] = useState<{ name: string; n: number } | null>(null);
  const lines = useQuery({
    queryKey: ['sheet', sheetId, 'audit', shown?.name ?? '', shown?.n ?? 0],
    queryFn: () =>
      getJson<string[]>(`/api/sheets/${encodeURIComponent(sheetId)}/audit?name=${encodeURIComponent(shown!.name)}`),
    enabled: shown !== null,
  });

  return (
    <section>
      <h2>Audit</h2>
      <input
        data-testid="audit-filter"
        placeholder="Name (blank for all)"
        value={filter}
        onChange={(e) => setFilter(e.target.value)}
      />{' '}
      <button
        type="button"
        data-testid="audit-show"
        onClick={() => setShown((s) => ({ name: filter.trim(), n: (s?.n ?? 0) + 1 }))}
      >
        Show
      </button>
      {shown && lines.data && (
        <ol>
          {lines.data.map((line, i) => (
            <li key={i} data-testid="audit-line">
              {line}
            </li>
          ))}
        </ol>
      )}
    </section>
  );
}

export const contribution = SheetBody.fill({ order: 90, Component: AuditViewer });
