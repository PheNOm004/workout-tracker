export interface AccountDeletionServices {
  disableReports(uid: string): Promise<void>;
  deleteOwnedData(uid: string): Promise<void>;
  deleteAuthentication(uid: string): Promise<void>;
}

export async function deleteOwnedAccount(uid: string, services: AccountDeletionServices): Promise<void> {
  if (!uid) throw new Error("authenticated uid required");
  await services.disableReports(uid);
  await services.deleteOwnedData(uid);
  await services.deleteAuthentication(uid);
}
