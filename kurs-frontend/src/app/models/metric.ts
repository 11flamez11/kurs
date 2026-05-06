export interface Metric {
  id: number;
  deviceId: number;
  cpuLoad: number;
  ramUsage: number;
  diskUsage: number;
  uploadMbps?: number;
  downloadMbps?: number;
  pingMs?: number;
  packetLossPercent?: number;
  uptime?: number;
  timestamp: string;
}
