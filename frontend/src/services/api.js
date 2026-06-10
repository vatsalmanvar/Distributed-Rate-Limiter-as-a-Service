import axios from 'axios';

const baseURL = import.meta.env.VITE_API_BASE_URL || '/api/v1';

export const api = axios.create({ baseURL });

export const wsUrl = (tenantId) => {
  const configured = import.meta.env.VITE_WS_URL;
  const base = configured || `${window.location.protocol === 'https:' ? 'wss' : 'ws'}://${window.location.host}/ws/analytics`;
  return tenantId ? `${base}?tenantId=${tenantId}` : base;
};

export const createTenant = (payload) => api.post('/tenants', payload).then((res) => res.data);
export const listTenants = () => api.get('/tenants').then((res) => res.data);
export const createRule = (tenantId, payload) => api.post(`/tenants/${tenantId}/rules`, payload).then((res) => res.data);
export const listRules = (tenantId) => api.get(`/tenants/${tenantId}/rules`).then((res) => res.data);
export const deleteRule = (tenantId, ruleId) => api.delete(`/tenants/${tenantId}/rules/${ruleId}`);
export const analyticsSummary = (tenantId, since) => api.get(`/analytics/tenants/${tenantId}/summary`, { params: { since } }).then((res) => res.data);
export const analyticsEvents = (tenantId) => api.get(`/analytics/tenants/${tenantId}/events`).then((res) => res.data);
export const checkRateLimit = (apiKey, payload) => api.post('/rate-limit/check', payload, { headers: { 'X-API-Key': apiKey } }).then((res) => res.data);
