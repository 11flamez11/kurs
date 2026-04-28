export interface Event {
  id: number;
  deviceId: number;
  severity: string;
  description: string;
  resolved: boolean;
  timestamp?: string;
}
