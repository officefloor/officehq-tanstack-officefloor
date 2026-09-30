// Work out an invoice's status from the payments recorded against it — no longer flipped to paid by
// hand. The server sends each invoice's amount and its amountDue (amount minus every payment); the
// status follows from what is still owed:
//   PAID    — nothing left to pay (payments cover the amount)
//   PARTIAL — some has been paid but not all
//   else    — nothing paid yet, so its lifecycle status stands (DRAFT until sent, then SENT)
// Kept in one shared place so the project invoices row and the invoice-detail panel read the same
// derived value (the test contract: SENT, PARTIAL, PAID).
export function invoiceStatus(invoice: {
  amount: number;
  amountDue: number;
  status: string;
}): string {
  const amount = Number(invoice.amount);
  const due = Number(invoice.amountDue);
  if (amount > 0 && due <= 0) {
    return 'PAID';
  }
  if (due < amount) {
    return 'PARTIAL';
  }
  return invoice.status;
}
