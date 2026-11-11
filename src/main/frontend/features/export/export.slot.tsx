import { SheetBody } from '../../slots/defs/sheetBody';
import { useClaims } from '../claims/claims';
import { gradeOn, liveHolds, usePeople } from '../people/people';
import { claimDue, heldParts, personDueTotal, rateOn, useRates } from '../rates/rates';
import { periodOf, usePeriods } from '../periods/periods';
import { useCap, useFloor, useLate } from '../access/access';
import { useSheetToday } from '../sheet/sheet';
import { getJson } from '../../api/http';
import type { Pool } from '../pools/pools';
import { downloadCsv } from './csv';
import { useCutTotals } from '../pools/pools';

// Downloads the people as a CSV file: a header line, then one line per person in list order,
// retired people included, with the grade in force today, the retired-from date ('' when not
// retired) and the due-total as the list shows it. Lines end with a line feed. While an as-of date
// is set every figure is as of it, as the list shows it then.
function ExportPeople({ sheetId }: { sheetId: string }) {
  const people = usePeople(sheetId).data ?? [];
  const claims = useClaims(sheetId).data ?? [];
  const rates = useRates(sheetId).data ?? [];
  const today = useSheetToday(sheetId);
  const floor = useFloor(sheetId);
  const late = useLate(sheetId);
  const cap = useCap(sheetId);
  const cutOf = useCutTotals(sheetId);

  const download = () => {
    const lines = [['name', 'grade', 'retired-from', 'due-total']];
    for (const p of people) {
      const own = claims.filter((c) => c.personId === p.id);
      lines.push([p.name, p.grade, p.retiredFrom, personDueTotal(rates, p.grades, today, own, liveHolds(p, today), floor, late, cap, heldParts(p.id, people, claims, rates, today, floor, late), cutOf(p.id))]);
    }
    downloadCsv(`people-${sheetId}.csv`, lines);
  };

  return (
    <p>
      <button type="button" data-testid="export-people" onClick={download}>
        Export people (CSV)
      </button>
    </p>
  );
}

// Downloads the claims as a CSV file: a header line, then one line per claim, people in list order
// (retired people included) and each person's claims oldest first, with the number, due and period
// as the claim's row shows them. Quoted and ended as the people export.
function ExportClaims({ sheetId }: { sheetId: string }) {
  const people = usePeople(sheetId).data ?? [];
  const claims = useClaims(sheetId).data ?? [];
  const rates = useRates(sheetId).data ?? [];
  const periods = usePeriods(sheetId).data ?? [];
  const floor = useFloor(sheetId);
  const late = useLate(sheetId);

  const download = () => {
    const lines = [['person', 'number', 'date', 'amount', 'due', 'status', 'period']];
    for (const p of people) {
      // The sheet's claims come newest first.
      const own = claims.filter((c) => c.personId === p.id).reverse();
      for (const c of own) {
        const grade = gradeOn(p.grades, c.date);
        const due = claimDue(c, rateOn(rates, grade, c.date, c.id), floor, late, grade);
        lines.push([p.name, c.number, c.date, c.amount, due, c.status, periodOf(periods, c.dueBy)?.name ?? '']);
      }
    }
    downloadCsv(`claims-${sheetId}.csv`, lines);
  };

  return (
    <p>
      <button type="button" data-testid="export-claims" onClick={download}>
        Export claims (CSV)
      </button>
    </p>
  );
}

// Downloads the pools as a CSV file: a header line, then one line per member, pools in the order
// added and each pool's members in the order added, a member pool as pool:<name> and the cut as
// the pool's row shows it ('' when none). Quoted and ended as the people export. The pools are read
// afresh when pressed, so a change just made is always in the file; while an as-of date is set they
// are read as of it (members, pot and cuts as they stood then).
function ExportPools({ sheetId }: { sheetId: string }) {
  const download = async () => {
    const { pools } = await getJson<{ pools: Pool[] }>(`/api/sheets/${encodeURIComponent(sheetId)}/pools`);
    const lines = [['pool', 'member', 'share', 'cut']];
    for (const p of [...pools].sort((a, b) => a.added - b.added)) {
      for (const m of p.members) {
        lines.push([p.name, m.memberPool ? `pool:${m.memberPool}` : m.name, m.share, m.cut]);
      }
    }
    downloadCsv(`pools-${sheetId}.csv`, lines);
  };

  return (
    <p>
      <button type="button" data-testid="export-pools" onClick={() => void download()}>
        Export pools (CSV)
      </button>
    </p>
  );
}

export const contributions = [
  SheetBody.fill({ order: 2, Component: ExportPeople }),
  SheetBody.fill({ order: 2.1, Component: ExportClaims }),
  SheetBody.fill({ order: 2.2, Component: ExportPools }),
];
