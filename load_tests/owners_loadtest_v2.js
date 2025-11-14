/**
 * Owners Load Test v2 (k6) – PetClinic Serverless (AWS Lambda + API Gateway)
 * ---------------------------------------------------------------------------
 * Objetivo acadêmico: cenários metodologicamente robustos para avaliar
 * elasticidade, escalabilidade, estabilidade, desempenho sustentado, resiliência,
 * concorrência de escrita e cold start real. Resultados auditáveis e reprodutíveis.
 *
 * Como rodar (PowerShell):
 *   $env:SCENARIO="spike";   k6 run -e BASE_URL=https://<api> load_tests/owners_loadtest_v2.js
 *   $env:SCENARIO="ramp";    k6 run -e BASE_URL=https://<api> load_tests/owners_loadtest_v2.js
 *   $env:SCENARIO="soak";    k6 run -e BASE_URL=https://<api> load_tests/owners_loadtest_v2.js
 *   $env:SCENARIO="peak";    k6 run -e BASE_URL=https://<api> load_tests/owners_loadtest_v2.js
 *   $env:SCENARIO="stress";  k6 run -e BASE_URL=https://<api> load_tests/owners_loadtest_v2.js
 *   $env:SCENARIO="critical";k6 run -e BASE_URL=https://<api> load_tests/owners_loadtest_v2.js
 *   $env:SCENARIO="cold";    k6 run -e BASE_URL=https://<api> load_tests/owners_loadtest_v2.js
 *
 * Cenários predefinidos (VU máx e duração padrão):
 *   - critical → carga estável e moderada
 *       • executor: constant-vus, vus=20, duration=5m
 *   - soak → longa duração (teste de resistência/estabilidade)
 *       • executor: constant-vus, vus=30, duration=1h
 *   - spike → picos curtos e repentinos com aquecimento e resfriamento
 *       • executor: ramping-vus, picos até 50 VUs
 *   - ramp → rampa gradual com platôs intermediários
 *       • executor: ramping-vus, até 100 VUs (platôs em 20/60)
 *   - peak → pico sustentado
 *       • executor: constant-vus, vus=100, duration=10m
 *   - stress → exploração de limites (não solicitado, mas disponível)
 *       • executor: ramping-vus, até 500 VUs em estágios
 *   - cold → latência a frio (1 VU, 5 iterações com pausas longas)
 *
 * Atributo de qualidade por cenário:
 *   - Spike   → Elasticidade
 *   - Ramp    → Escalabilidade
 *   - Soak    → Estabilidade
 *   - Peak    → Desempenho sustentado
 *   - Stress  → Resiliência / Limites
 *   - Critical→ Consistência sob concorrência de escrita
 *   - Cold    → Tempo de inicialização (cold start)
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Counter, Rate } from 'k6/metrics';
import { textSummary } from 'https://jslib.k6.io/k6-summary/0.0.4/index.js';

// Config ---------------------------------------------------------
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
// Alguns backends (ex.: PetClinic REST) exigem telefone com 10 dígitos
// Permite ajustar via variável de ambiente PHONE_LEN (default 10 p/ compatibilidade com monólito REST)
const PHONE_LEN = Number(__ENV.PHONE_LEN || 10);
const SCENARIO = (__ENV.SCENARIO || 'spike').toLowerCase();
const DEBUG = (__ENV.DEBUG || 'false').toLowerCase() === 'true';
const SPIKE_RPS = Number(__ENV.SPIKE_RPS || 120); // legado (não mais usado quando spike=VU-based)
const START_TS = Date.now(); // para logs intermediários no ramp
const SEED_COUNT = Number(__ENV.SEED_COUNT || 500); // permitir reduzir a semeadura local

// Métricas customizadas globais
export const owners_latency = new Trend('owners_latency', true);
export const owners_success = new Rate('owners_success');
export const owners_errors = new Counter('owners_errors');
export const owners_errors_get = new Counter('owners_errors_get');
export const owners_errors_post = new Counter('owners_errors_post');
export const owners_errors_put = new Counter('owners_errors_put');
// breakdown por status
export const owners_status_2xx = new Counter('owners_status_2xx');
export const owners_status_3xx = new Counter('owners_status_3xx');
export const owners_status_4xx = new Counter('owners_status_4xx');
export const owners_status_5xx = new Counter('owners_status_5xx');
export const owners_404 = new Counter('owners_404');
export const owners_409 = new Counter('owners_409');
export const owners_429 = new Counter('owners_429');
export const owners_5xx = new Counter('owners_5xx');

// Pesos CRUD padrão (em %)
const DEFAULT_WEIGHTS = [
  { name: 'GET_LIST', w: 50 },
  { name: 'GET_BY_ID', w: 20 },
  { name: 'POST', w: 15 },
  { name: 'PUT', w: 10 },
  { name: 'DELETE', w: 5 },
];

// Helpers --------------------------------------------------------
function randInt(min, max) { return Math.floor(Math.random() * (max - min + 1)) + min; }

function alphaToken(len = 6) {
  const letters = 'abcdefghijklmnopqrstuvwxyz';
  let out = '';
  for (let i = 0; i < len; i++) out += letters[Math.floor(Math.random() * letters.length)];
  return out;
}

function uniquePhone(seq) {
  // Número único de N dígitos (PHONE_LEN); usa __VU/__ITER quando disponíveis; em setup usa seq
  const now = Date.now();
  let suff = '';
  if (typeof __VU !== 'undefined' && typeof __ITER !== 'undefined') suff = `${__VU}${__ITER}`;
  else if (typeof seq !== 'undefined') suff = `s${seq}`;
  else suff = `${Math.floor(Math.random() * 1e6)}`;
  const raw = `${now}${suff}`.replace(/\D/g, '');
  return raw.slice(-PHONE_LEN).padStart(PHONE_LEN, '9');
}

export function randomOwner(overrides = {}, seq) {
  // Restrições do PetClinic REST (OwnerFieldsDto.firstName):
  // deve casar com regex ^[\p{L}]+([ '-][\p{L}]+){0,2}$ (apenas letras, com até dois separadores)
  // Mantemos nomes curtos e somente letras para evitar 400.
  const first = `T${alphaToken(6)}`; // ex.: 'Tabcxyz' (apenas letras)
  return Object.assign({
    firstName: first,
    lastName: 'Load', // somente letras
    address: `Rua ${randInt(1, 9999)}`,
    city: 'Curitiba',
    telephone: uniquePhone(seq),
  }, overrides);
}

function pickWeightedAction(weights) {
  const total = weights.reduce((a, b) => a + b.w, 0);
  let r = Math.random() * total;
  for (const item of weights) { if ((r -= item.w) <= 0) return item.name; }
  return weights[weights.length - 1].name;
}

// HTTP -----------------------------------------------------------
// Estatísticas aproximadas para logs intermediários (VU 1)
let VU1_REQ = 0;
let VU1_OK = 0;
let VU1_DUR = [];
let LAST_LOG_TS = START_TS;
let LAST_LOG_REQ = 0;
// [Melhoria TCC] Contadores locais para VU 1 (2xx/5xx) usados em detecção de saturação
let VU1_2XX = 0;
let VU1_5XX = 0;
let LAST_SAT_LOG_TS = 0;
const PLATEAU_LOG_POINTS = [
  { at: 2 * 60 * 1000 + 30 * 1000, label: 'plateau-20' }, // 2m30s
  { at: 6 * 60 * 1000 + 60 * 1000, label: 'plateau-60' }, // 7m (meio do platô 60)
];
const LOGGED_POINTS = new Set();

function maybeLogRampPlateau() {
  if (SCENARIO !== 'ramp' || typeof __VU === 'undefined' || __VU !== 1) return;
  const now = Date.now();
  const elapsed = now - START_TS;
  for (const p of PLATEAU_LOG_POINTS) {
    if (elapsed >= p.at && !LOGGED_POINTS.has(p.label)) {
      const dt = (now - LAST_LOG_TS) / 1000;
      const dreq = VU1_REQ - LAST_LOG_REQ;
      const thr = dt > 0 ? (dreq / dt).toFixed(2) : '0.00';
      const succ = VU1_REQ > 0 ? ((VU1_OK / VU1_REQ) * 100).toFixed(2) : '0.00';
      // p95 aproximado no VU 1
      const arr = VU1_DUR.slice().sort((a,b)=>a-b);
      const idx = Math.floor(0.95 * (arr.length - 1));
      const p95 = arr.length ? arr[Math.max(0, idx)].toFixed(2) : '0.00';
      console.log(`[ramp][${p.label}] ~throughput≈${thr} req/s, sucesso≈${succ}%, p95≈${p95} ms (VU1 aprox)`);
      LAST_LOG_TS = now;
      LAST_LOG_REQ = VU1_REQ;
      LOGGED_POINTS.add(p.label);
    }
  }
}

function resilientRequest(req) {
  let res;
  for (let i = 0; i < 3; i++) {
    res = http.request(req.method, req.url, req.body, req.params);
    if (res.status >= 200 && res.status < 300) break;
    // [Melhoria TCC] Backoff exponencial leve para reduzir rajadas simultâneas sob falha
    sleep(Math.pow(2, i) * 0.1 + Math.random() * 0.2);
  }
  // coleta aproximada só no VU 1 para logs intermediários
  try {
    if (typeof __VU !== 'undefined' && __VU === 1) {
      VU1_REQ += 1;
      const ok = res && res.status >= 200 && res.status < 300;
      if (ok) VU1_OK += 1;
      if (res && res.timings && typeof res.timings.duration === 'number') {
        VU1_DUR.push(res.timings.duration);
        if (VU1_DUR.length > 5000) VU1_DUR.shift();
      }
      maybeLogRampPlateau();
    }
  } catch (_) {}
  return res;
}

function request(method, path, body, tags = {}) {
  const url = `${BASE_URL}${path}`;
  const params = { headers: { 'Content-Type': 'application/json', 'Accept': 'application/json' }, tags: { method, endpoint: path, scenario: SCENARIO, ...tags } };
  const payload = body ? JSON.stringify(body) : null;
  const res = resilientRequest({ method, url, body: payload, params });
  const ok = check(res, {
    'status is 2xx': (r) => r.status >= 200 && r.status < 300,
    'status is not 5xx': (r) => r.status < 500,
  });
  owners_success.add(ok);
  if (!ok) {
    owners_errors.add(1);
    if (method === 'GET') owners_errors_get.add(1);
    else if (method === 'POST') owners_errors_post.add(1);
    else if (method === 'PUT') owners_errors_put.add(1);
    if (DEBUG) console.log(`[ERR] ${method} ${path} => ${res.status}`);
    // capture a limited sample of error payloads for diagnosis
    try {
      if (ERROR_SAMPLES.length < ERROR_SAMPLES_MAX) {
        const rid = res.headers['x-amzn-RequestId'] || res.headers['x-amzn-trace-id'] || res.headers['x-amz-apigw-id'] || '';
        const bodyStr = (() => {
          try { return typeof res.body === 'string' ? res.body : JSON.stringify(res.json()); } catch (_) { return String(res.body || ''); }
        })();
        ERROR_SAMPLES.push({ ts: Date.now(), method, path, status: res.status, requestId: rid, body: String(bodyStr).slice(0, 256) });
      }
    } catch (_) {}
  }
  // status breakdown
  if (res.status >= 200 && res.status < 300) owners_status_2xx.add(1);
  else if (res.status >= 300 && res.status < 400) owners_status_3xx.add(1);
  else if (res.status >= 400 && res.status < 500) {
    owners_status_4xx.add(1);
    if (res.status === 404) owners_404.add(1);
    if (res.status === 409) owners_409.add(1);
    if (res.status === 429) owners_429.add(1);
  } else if (res.status >= 500) {
    owners_status_5xx.add(1);
    owners_5xx.add(1);
  }
  // [Melhoria TCC] Atualiza contadores locais (VU 1) para detector de saturação
  try {
    if (typeof __VU !== 'undefined' && __VU === 1) {
      if (res.status >= 200 && res.status < 300) VU1_2XX += 1;
      else if (res.status >= 500) VU1_5XX += 1;
    }
  } catch (_) {}
  owners_latency.add(res.timings.duration);
  return res;
}

// [Melhoria TCC] Tags estáticas (name/endpoint) em todos os requests para evitar alta cardinalidade
function getOwners()       { return request('GET', '/owners', null, { name: 'GET /owners', endpoint: '/owners' }); }
function getOwnerById(id)  { return request('GET', `/owners/${id}`, null, { name: 'GET /owners/{id}', endpoint: '/owners/{id}' }); }
function createOwner(o)    { const r = request('POST', '/owners', o, { name: 'POST /owners', endpoint: '/owners' }); return r; }
function updateOwner(id,o) { return request('PUT', `/owners/${id}`, o, { name: 'PUT /owners/{id}', endpoint: '/owners/{id}' }); }
function deleteOwner(id)   { return request('DELETE', `/owners/${id}`, null, { name: 'DELETE /owners/{id}', endpoint: '/owners/{id}' }); }

// Cenários -------------------------------------------------------
function scenarioOptions(name) {
  const thresholdsDefault = {
    http_req_failed: [{ threshold: 'rate<0.01', abortOnFail: false, delayAbortEval: '45s' }],
    checks: ['rate>0.99'],
    http_req_duration: ['p(95)<400'],
  };
  const thresholdsRamp = {
    http_req_failed: [{ threshold: 'rate<0.1', abortOnFail: false, delayAbortEval: '1m' }],
    checks: ['rate>0.95'],
    http_req_duration: ['p(95)<400'],
  };

  const options = {
    // Spike reescrito em VUs com warm-up progressivo e 3 picos com pausas quentes (~2m20s)
    spike: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        // warm-up progressivo
        { duration: '30s', target: 10 },
        { duration: '20s', target: 30 },
        // spikes com pausas quentes (10 VUs)
  { duration: '10s', target: 50 }, // spike 1
        { duration: '10s', target: 0 },
  { duration: '40s', target: 10 },
  { duration: '10s', target: 50 }, // spike 2
        { duration: '10s', target: 0 },
  { duration: '40s', target: 10 },
  { duration: '10s', target: 50 }, // spike 3
        { duration: '10s', target: 0 },
      ],
      gracefulRampDown: '10s',
    },
    // RAMP reestruturado: ramp-up progressivo com platôs e cooldown
    ramp: {
      exec: 'rampExec',
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '2m', target: 20 },
        { duration: '1m', target: 20 }, // plateau 20 VUs
        { duration: '3m', target: 60 },
        { duration: '2m', target: 60 }, // plateau 60 VUs
        { duration: '3m', target: 100 },
        { duration: '2m', target: 0 },
      ],
      gracefulRampDown: '30s',
    },
    soak: {
      executor: 'constant-vus', vus: 30, duration: '1h', gracefulStop: '30s',
    },
    peak: {
      executor: 'constant-vus', vus: 100, duration: '10m', gracefulStop: '30s',
    },
    stress: {
      executor: 'ramping-vus', startVUs: 10,
      stages: [
        { duration: '2m', target: 100 },
        { duration: '2m', target: 200 },
        { duration: '2m', target: 300 },
        { duration: '2m', target: 400 },
        { duration: '2m', target: 500 },
      ],
      gracefulRampDown: '30s',
    },
    critical: {
      executor: 'constant-vus', vus: 20, duration: '5m', gracefulStop: '30s',
    },
    cold: {
      // 1 VU, 5 iterações com pausas longas internas (15m) para simular cold start real
      executor: 'shared-iterations', vus: 1, iterations: 5, maxDuration: '2h',
    },
  };

  const thresholds = (name === 'ramp') ? thresholdsRamp : thresholdsDefault;
  return { scenarios: { owners: { exec: (name === 'ramp') ? 'rampExec' : 'default', ...options[name] } }, thresholds };
}

const SETUP_TIMEOUT_SECONDS = Number(__ENV.SETUP_TIMEOUT_SECONDS || 120);
export const options = {
  ...scenarioOptions(SCENARIO),
  // [Melhoria TCC] Aumenta tolerância de setup para evitar timeout durante seeding ampliado
  setupTimeout: `${SETUP_TIMEOUT_SECONDS}s`,
};

// Semeadura ------------------------------------------------------
const CREATED_IDS = [];
const ERROR_SAMPLES_MAX = Number(__ENV.ERROR_SAMPLES_MAX || 50);
const ERROR_SAMPLES = [];
let LAST_SEED_IDS = [];
export function setup() {
  // Semear base para GET by id / PUT / DELETE com telefones únicos
  // [Melhoria TCC] Seed ampliado para 500 IDs para reduzir 404 sob carga e melhorar reprodutibilidade
  const seedCount = SEED_COUNT; // controlável por env var
  const seedIds = [];
  for (let i = 0; i < seedCount; i++) {
    const owner = randomOwner({}, i);
    const res = createOwner(owner);
    try { const data = res.json(); if (data && data.id !== undefined) seedIds.push(data.id); } catch (_) {}
    sleep(0.10);
  }
  LAST_SEED_IDS = seedIds.slice();
  console.log(`Seed concluído com ${seedIds.length} owners`);
  return { baseUrl: BASE_URL, seedIds };
}

// Funções auxiliares específicas do RAMP ------------------------
function pickStableOwnerId(seedIds) {
  if (Array.isArray(seedIds) && seedIds.length > 0) {
    const pool = seedIds.slice(0, Math.min(5, seedIds.length));
    const idx = Math.floor(Math.random() * pool.length);
    return pool[idx];
  }
  return null;
}

// Execução do cenário RAMP: reduz cardinalidade e mede escalabilidade de leitura
export function rampExec(data) {
  const SEED = Array.isArray(data?.seedIds) ? data.seedIds : [];
  const r = Math.random();
  if (r < 0.7) {
    // GET /owners com tag estática
    request('GET', '/owners', null, { name: 'GET /owners', endpoint: '/owners' });
  } else {
    const id = pickStableOwnerId(SEED);
    if (id != null) {
      // GET /owners/{id} com tag estática para evitar cardinalidade de path
      request('GET', `/owners/${id}`, null, { name: 'GET /owners/{id}', endpoint: '/owners/{id}' });
    } else {
      request('GET', '/owners', null, { name: 'GET /owners', endpoint: '/owners' });
    }
  }
  // pequena pausa ajuda estabilidade
  sleep(0.2);
}

function pickActionName() {
  if (SCENARIO === 'critical') return pickWeightedAction([
    { name: 'POST', w: 70 }, { name: 'PUT', w: 30 },
  ]);
  return pickWeightedAction(DEFAULT_WEIGHTS);
}

// Execução -------------------------------------------------------
export default function (data) {
  if (SCENARIO === 'cold') {
    // Chamada isolada e pausa grande (15 minutos)
    getOwners();
    sleep(15 * 60);
    return;
  }

  // [Melhoria TCC] Log de saturação automática (VU 1) a cada ~5s
  try {
    if (typeof __VU !== 'undefined' && __VU === 1) {
      const now = Date.now();
      if (!LAST_SAT_LOG_TS || (now - LAST_SAT_LOG_TS) > 5000) {
        if (VU1_5XX > VU1_2XX * 0.2) {
          console.log(`[PEAK] Saturação detectada (~${VU1_5XX} 5xx até agora)`);
        }
        LAST_SAT_LOG_TS = now;
      }
    }
  } catch (_) {}

  const action = pickActionName();
  const SEED = (data && Array.isArray(data.seedIds)) ? data.seedIds : [];
  const ID_POOL = SEED.concat(CREATED_IDS);
  switch (action) {
    case 'GET_LIST':
      getOwners();
      break;
    case 'GET_BY_ID':
      if (CREATED_IDS.length > 0) {
        getOwnerById(CREATED_IDS[randInt(0, CREATED_IDS.length - 1)]);
      } else if (SEED.length > 0) {
        getOwnerById(SEED[randInt(0, SEED.length - 1)]);
      } else {
        getOwners();
      }
      break;
    case 'POST': {
      const res = createOwner(randomOwner());
      try { const d = res.json(); if (d && d.id !== undefined) CREATED_IDS.push(d.id); } catch (_) {}
      break;
    }
    case 'PUT': {
      if (CREATED_IDS.length > 0) {
        const id = CREATED_IDS[randInt(0, CREATED_IDS.length - 1)];
        updateOwner(id, randomOwner());
      } else if (SEED.length > 0) {
        const id = SEED[randInt(0, SEED.length - 1)];
        updateOwner(id, randomOwner());
      } else {
        const res = createOwner(randomOwner());
        try { const d = res.json(); if (d && d.id !== undefined) updateOwner(d.id, randomOwner()); } catch (_) {}
      }
      break;
    }
    case 'DELETE': {
      // Protege IDs semeados: só deletamos IDs criados durante o teste
      if (CREATED_IDS.length > 0) {
        const idx = randInt(0, CREATED_IDS.length - 1);
        const id = CREATED_IDS[idx];
        const r = deleteOwner(id);
        if (r.status >= 200 && r.status < 300) {
          CREATED_IDS.splice(idx, 1);
        }
      } else {
        // Sem IDs para deletar; evita apagar seed. Faz uma leitura para manter ritmo.
        getOwners();
      }
      break;
    }
  }

  sleep(0.2);
}

// Sumário / Auditoria -------------------------------------------
export function handleSummary(data) {
  const runId = `owners-${SCENARIO}-${new Date().toISOString().replace(/[:.]/g, '-')}`;
  const m = data.metrics || {};
  const dur = m.http_req_duration?.values || {};
  const p95 = dur['p(95)'];
  const avg = dur['avg'];
  const success = m.owners_success?.values?.rate;
  const errGet = m.owners_errors_get?.values?.count || 0;
  const errPost = m.owners_errors_post?.values?.count || 0;
  const errPut = m.owners_errors_put?.values?.count || 0;

  const notes = [
    `Cenário: ${SCENARIO}`,
    `Latência (avg): ${avg?.toFixed ? avg.toFixed(2) : avg} ms`,
    `Latência (p95): ${p95?.toFixed ? p95.toFixed(2) : p95} ms`,
    `Sucesso global: ${success !== undefined ? (success * 100).toFixed(2) + '%' : 'n/a'}`,
    `Erros por método: GET=${errGet} POST=${errPost} PUT=${errPut}`,
  ].join('\n');

  const txt = `${textSummary(data, { indent: ' ', enableColors: true })}\n\n${notes}\n`;
  const json = JSON.stringify({ summary: data, notes, errorSamples: ERROR_SAMPLES }, null, 2);
  return {
    stdout: `\n--- Summary (${runId}) ---\n${txt}`,
    [`load_tests/summary-${runId}.txt`]: txt,
    [`load_tests/summary-${runId}.json`]: json,
    // Salva os IDs semeados para reuso futuro
    ['load_tests/seed-owners.json']: JSON.stringify({ seedIds: LAST_SEED_IDS }, null, 2),
  };
}
