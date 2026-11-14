import { useQuery } from '@tanstack/react-query';
import { SheetBody } from '../../slots/defs/sheetBody';
import { useSheetId, useUser } from '../session/session';
import { openSheet, sheetKey } from './api';

/** The loaded sheet: its id, then everything kept on it (the sheet.body region). */
export function ActiveSheet() {
  const [user] = useUser();
  const [sheetId] = useSheetId();
  const sheet = useQuery({
    queryKey: sheetKey(sheetId),
    queryFn: () => openSheet(sheetId, user),
    enabled: sheetId !== '',
  });

  if (sheetId === '') return <p data-testid="home-empty">No sheet loaded.</p>;
  if (sheet.isError) return <p data-testid="sheet-error">The sheet could not be loaded.</p>;
  if (!sheet.data) return <p data-testid="sheet-loading">Loading…</p>;

  return (
    <section>
      <h2>
        Sheet <span data-testid="sheet-current-id">{sheet.data.id}</span>
      </h2>
      <SheetBody.Slot sheetId={sheet.data.id} user={user} />
    </section>
  );
}
