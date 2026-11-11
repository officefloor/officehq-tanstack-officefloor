import { SheetBody } from '../../slots/defs/sheetBody';
import { useClaims } from '../claims/claims';
import { gradeOn, liveHolds, usePeople } from '../people/people';
import { claimDue, personDueTotal, rateOn, useRates } from '../rates/rates';
import { periodOf, usePeriods } from '../periods/periods';
import { useCap, useFloor, useLate } from '../access/access';
import { useSheetToday } from '../sheet/sheet';

// A CSV value: wrapped in double quotes (any quote inside doubled) when it holds a comma, a quote
// or a line break.
const csvValue = (v: string) => (/[",\r\n]/.test(v) ? `"${v.replace(/"/g, '""')}"` : v);

// Downloads `lines` as a CSV file named `name`, each line ending with a line feed.
function downloadCsv(name: string, lines: string[][]) {
  const csv = lines.map((l) => l.map(csvValue).join(',') + '\n').join('');
  const url = URL.createObjectURL(new Blob([csv], { type: 'text/csv' }));
  const a = document.createElement('a');
  a.href = url;
  a.download = name;
  document.body.appendChild(a);
  a.click();
  a.remove();
  setTimeout(() => URL.revokeObjectURL(url), 1000);
}

// Downloads the people as a CSV file: a header line, then one line per person in list order,
// retired people included, with the grade in force today, the retired-from date ('' when not
// retired) and the due-total as the list shows it. Lines end with a line feed.
function ExportPeople({ sheetId }: { sheetId: string }) {
  const people = usePeople(sheetId).data ?? [];
  const claims = useClaims(sheetId).data ?? [];
  const rates = useRates(sheetId).data ?? [];
  const today = useSheetToday(sheetId);
  const floor = useFloor(sheetId);
  const late = useLate(sheetId);
  const cap = useCap(sheetId);

  const download = () => {
    const lines = [['name', 'grade', 'retired-from', 'due-total']];
    for (const p of people) {
      const own = claims.filter((c) => c.personId === p.id);
      lines.push([p.name, p.grade, p.retiredFrom, personDueTotal(rates, p.grades, today, own, liveHolds(p, today), floor, late, cap)]);
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
        const due = claimDue(c, rateOn(rates, grade, c.date), floor, late, grade);
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


export const contributions = [
  SheetBody.fill({ order: 2, Component: ExportPeople }),
  SheetBody.fill({ order: 2.1, Component: ExportClaims }),
];
