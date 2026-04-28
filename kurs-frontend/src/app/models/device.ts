export interface Device {
  id: number;
  hostname: string;
  ipAddress: string;
  os: string;
  status: string;
  lastCheck?: string;
  userId?: number;
}
