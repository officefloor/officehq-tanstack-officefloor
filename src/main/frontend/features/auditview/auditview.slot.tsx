import { useState } from 'react';
import { useMutation } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { HomeMain } from '../../slots/defs/homeMain';
import { useCurrentSheet, useCurrentUser } from '../session/session';

const auditPath = (sheetId: string, name: string) =>
  `/api/sheets/${encodeURIComponent(sheetId)}/audit?name=${encodeURIComponent(name)}`;

// The audit viewer: the sheet's audit lines exactly as the audit file holds them, newest first, or only those
// about the person named in the filter. It reads the file each time Show is pressed; it keeps no record of its
// own. While a read runs the old lines are gone, so two reads are always told apart.
function AuditViewer() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const [filter, setFilter] = useState('');
  const show = useMutation({
    mutationFn: (name: string) => getJson<string[]>(auditPath(sheet ?? '', name)),
  });

  if (!user || !sheet) return null;
  return (
    <section data-testid="audit-viewer">
      <h2>Audit</h2>
      <form
        onSubmit={(e) => {
          e.preventDefault();
          show.mutate(filter);
        }}
      >
        <input
          data-testid="audit-filter"
          placeholder="Person name (blank for all)"
          value={filter}
          onChange={(e) => setFilter(e.target.value)}
        />
        <button type="submit" data-testid="audit-show">Show</button>
      </form>
      <ul>
        {(show.isPending ? [] : show.data ?? []).map((line, i) => (
          <li key={i} data-testid="audit-line">{line}</li>
        ))}
      </ul>
    </section>
  );
}

export const contribution = HomeMain.fill({ order: 99, Component: AuditViewer });
