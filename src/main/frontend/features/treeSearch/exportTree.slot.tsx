import { downloadCsv } from '../../api/csv';
import { getJson } from '../../api/http';
import { SheetPanel } from '../../slots/defs/sheetPanel';

/** Downloads the people of the whole tree as CSV: this sheet's people first in list order, then the other sheets' of
 *  its tree by sheet id, each with its grade and due-total. Read from the store when pressed. */
function ExportTree({ sheetId }: { sheetId: string }) {
  const exportCsv = async () => {
    const rows = await getJson<string[][]>(`/api/sheets/${encodeURIComponent(sheetId)}/tree-people`);
    downloadCsv('tree.csv', [['sheet', 'name', 'grade', 'due-total'], ...rows]);
  };

  return (
    <p>
      <button type="button" data-testid="export-tree" onClick={() => void exportCsv()}>
        Export tree people (CSV)
      </button>
    </p>
  );
}

export const contribution = SheetPanel.fill({ order: 39.1, Component: ExportTree });
