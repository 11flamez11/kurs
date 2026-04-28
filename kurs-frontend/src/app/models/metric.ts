export interface Metric {
  id: number;
  deviceId: number;
  cpuLoad: number;
  ramUsage: number;
  diskUsage: number;
  uptime?: number;
  timestamp: string;
}
