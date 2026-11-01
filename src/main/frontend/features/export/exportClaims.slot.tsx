import { SheetPanels } from '../../slots/defs/sheetPanels';
import { claimDueCents, useClaims } from '../claims/useClaims';
import { gradeOn, usePeople } from '../people/usePeople';
import { periodOf, usePeriods } from '../periods/usePeriods';
import { claimRateOn, showDue, useRates } from '../rates/useRates';
import { useFloor, useLate } from '../settings/useSettings';
import { downloadCsv, useSheetRead } from './csv';

/**
 * Downloads the claims as a CSV file: a header line, then one line per claim, people in list order
 * (retired people included, shown or not) and each person's claims oldest first, with the number,
 * date, amount, due, status and period as each claim row shows them — while an as-of date is set,
 * as of that date (the button waits for those figures). Quoted and ended as the people export.
 */
function ExportClaims({ sheet }: { user: string; sheet: string }) {
  const { data: people = [] } = usePeople(sheet);
  const { data: all = [] } = useClaims(sheet);
  const { data: rates = [] } = useRates(sheet);
  const { data: periods = [] } = usePeriods(sheet);
  const floor = useFloor(sheet);
  const lateBy = useLate(sheet);

  const read = useSheetRead(sheet);

  const download = () => {
    const lines = [['person', 'number', 'date', 'amount', 'due', 'status', 'period']];
    for (const p of people) {
      // Claims come newest first: reversed, oldest first.
      const claims = all.filter((c) => c.personId === p.id).reverse();
      for (const c of claims) {
        lines.push([
          p.name,
          c.number ?? '',
          c.date,
          c.amount,
          showDue(claimDueCents(c, floor, claimRateOn(rates, gradeOn(p, c.date), c), lateBy)),
          c.status,
          periodOf(periods, c.dueBy)?.name ?? '',
        ]);
      }
    }
    downloadCsv(lines, `claims-${sheet}.csv`);
  };

  return (
    <p>
      <button type="button" data-testid="export-claims" disabled={!read} onClick={download}>
        Export claims (CSV)
      </button>
    </p>
  );
}

export const contribution = SheetPanels.fill({ order: 12, Component: ExportClaims });
