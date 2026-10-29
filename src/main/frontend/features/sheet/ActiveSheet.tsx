import { useQuery } from '@tanstack/react-query';
import { SheetPanel } from '../../slots/defs/sheetPanel';
import { forSlot } from '../../slots/registry';
import { claimsQuery } from '../claims/claimsQuery';
import { notesQuery } from '../notes/notesQuery';
import { peopleQuery } from '../people/peopleQuery';
import { useSheetId } from './useSheetId';
import { sheetQuery } from './sheetQuery';

/** The loaded sheet: its id, then every per-sheet panel. Nothing shows until it, its people, its claims and its notes have loaded. */
export function ActiveSheet({ user }: { user: string }) {
  const [sheetId] = useSheetId();
  const sheet = useQuery({ ...sheetQuery(sheetId), enabled: sheetId !== '' });
  // Its people, claims and notes are read before it shows, so a loaded sheet never shows its rows filling in later.
  const people = useQuery({ ...peopleQuery(sheetId), enabled: sheetId !== '' });
  const claims = useQuery({ ...claimsQuery(sheetId), enabled: sheetId !== '' });
  const notes = useQuery({ ...notesQuery(sheetId), enabled: sheetId !== '' });

  if (sheetId === '') return <p data-testid="sheet-none">No sheet loaded.</p>;
  if (sheet.isError) return <p data-testid="sheet-load-error">Could not load sheet {sheetId}.</p>;
  if (!sheet.data || people.isPending || claims.isPending || notes.isPending) return <p data-testid="sheet-loading">Loading…</p>;

  return (
    <section data-testid="sheet">
      <h2>
        Sheet <span data-testid="sheet-current-id">{sheet.data.id}</span>
      </h2>
      <SheetPanel.Slot user={user} sheetId={sheet.data.id} />
      {forSlot(SheetPanel.id).length === 0 && <p data-testid="home-empty">Nothing here yet.</p>}
    </section>
  );
}
