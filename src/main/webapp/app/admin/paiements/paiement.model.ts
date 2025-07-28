export interface IPaiement {
  id: number;
  user?: string | null;
  amount?: number | null;
  currency?: string | null;
  date?: string | null;
  status?: string | null;
  cmiTransactionId?: string | null;
  cmiOrderId?: string | null;
  cmiResponseCode?: string | null;
  cmiResponseMessage?: string | null;
  cmiApprovalCode?: string | null;
  cmiMerchantId?: string | null;
  cmiPaymentMethod?: string | null;
  cmiCardType?: string | null;
  cmiCardNumber?: string | null;
  cmiCardHolder?: string | null;
  cmiInstallment?: string | null;
  cmi3DSecure?: string | null;
  cmiIpAddress?: string | null;
  cmiUserAgent?: string | null;
  cmiCreatedAt?: string | null;
  cmiUpdatedAt?: string | null;
  description?: string | null;
  clientIp?: string | null;
  createdAt?: string | null;
  updatedAt?: string | null;
}

export type NewPaiement = Omit<IPaiement, 'id'> & { id: null };
