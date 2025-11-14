/**
 * Owners Load Test (k6)
 * --------------------------------------------------------------
 * How to run (PowerShell examples):
 *
 *   # Spike (pico)
 *   $env:SCENARIO="spike"; k6 run -e BASE_URL=https://<api-id>.execute-api.us-east-1.amazonaws.com/dev load_tests/owners_loadtest.js
 *
 *   # Ramp-up (carga progressiva)
 *   $env:SCENARIO="ramp";  k6 run -e BASE_URL=https://<api-id>.execute-api.us-east-1.amazonaws.com/dev load_tests/owners_loadtest.js
 *
 *   # Soak (endurance)
 *   $env:SCENARIO="soak";  k6 run -e BASE_URL=https://<api-id>.execute-api.us-east-1.amazonaws.com/dev load_tests/owners_loadtest.js
 *
 *   # Stress extremo (break)
 *   $env:SCENARIO="stress"; k6 run -e BASE_URL=https://<api-id>.execute-api.us-east-1.amazonaws.com/dev load_tests/owners_loadtest.js
 *
 *   # Focado em críticos (GET list, POST, PUT)
 *   $env:SCENARIO="critical"; k6 run -e BASE_URL=https://<api-id>.execute-api.us-east-1.amazonaws.com/dev load_tests/owners_loadtest.js
 *
 *   # Cold start test (invocações espaçadas)
 *   $env:SCENARIO="cold"; k6 run -e BASE_URL=https://<api-id>.execute-api.us-east-1.amazonaws.com/dev load_tests/owners_loadtest.js
 *
 * - Ajuste BASE_URL para apontar para o monólito se desejar comparar.
 * - Métricas alvo: http_req_failed < 1%, http_req_duration p95 < 400ms.
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';
import { textSummary } from 'https://jslib.k6.io/k6-summary/0.0.4/index.js';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const SCENARIO = (__ENV.SCENARIO || 'spike').toLowerCase();

// Local metrics
export const ownersLatency = new Trend('owners_latency', true); // trend store
export const ownersSuccess = new Rate('owners_success');
export const ownersErrors = new Counter('owners_errors');

// Pools
const CREATED_IDS = []; // VU-local during execution; setup IDs virão via data.seedIds

// Utilities ------------------------------------------------------
function randInt(min, max) { // inclusive
  return Math.floor(Math.random() * (max - min + 1)) + min;
}

function uniquePhone(seq) {
  // Gera número único (11 dígitos) sem depender de __VU/__ITER em setup().
  // Usa timestamp + (__VU/__ITER quando disponíveis) ou índice de semente (seq).
  const now = Date.now();
  let suffix = '';
  if (typeof __VU !== 'undefined' && typeof __ITER !== 'undefined') {
    suffix = `${__VU}${__ITER}`;
  } else if (typeof seq !== 'undefined') {
    suffix = `s${seq}`;
  } else {
    suffix = `${Math.floor(Math.random() * 1e6)}`;
  }
  const raw = `${now}${suffix}`.replace(/\D/g, '');
  const digits = raw.slice(-11).padStart(11, '9');
  return digits;
}

function randomOwner(overrides = {}, seq) {
  const idSuffix = `${Date.now()}-${Math.floor(Math.random() * 1e6)}`;
  return Object.assign(
    {
      firstName: `Test${idSuffix}`,
      lastName: 'Owner',
      address: `Rua ${randInt(1, 9999)}`,
      city: 'Springfield',
      telephone: uniquePhone(seq),
    },
    overrides,
  );
}

function pickWeightedAction(weights) {
  // weights: [{name, w}]
  const total = weights.reduce((a, b) => a + b.w, 0);
  let r = Math.random() * total;
  for (const item of weights) {
    if ((r -= item.w) <= 0) return item.name;
  }
  return weights[weights.length - 1].name;
}

// HTTP helpers ---------------------------------------------------
function request(method, path, body, tags = {}) {
  const url = `${BASE_URL}${path}`;
  const params = { headers: { 'Content-Type': 'application/json' }, tags };
  const payload = body ? JSON.stringify(body) : null;
  const res = http.request(method, url, payload, params);
  const ok = check(res, {
    'status is 2xx': (r) => r.status >= 200 && r.status < 300,
  });
  ownersSuccess.add(ok);
  if (!ok) ownersErrors.add(1);
  ownersLatency.add(res.timings.duration);
  return res;
}

function getOwners() {
  return request('GET', '/owners', null, { endpoint: '/owners', method: 'GET' });
}

function getOwnerById(id) {
  return request('GET', `/owners/${id}`, null, { endpoint: '/owners/{id}', method: 'GET' });
}

function createOwner(owner) {
  const res = request('POST', '/owners', owner, { endpoint: '/owners', method: 'POST' });
  try {
    const data = res.json();
    if (data && (data.id !== undefined && data.id !== null)) {
      CREATED_IDS.push(data.id);
    }
  } catch (_) {}
  return res;
}

function updateOwner(id, owner) {
  return request('PUT', `/owners/${id}`, owner, { endpoint: '/owners/{id}', method: 'PUT' });
}

function deleteOwner(id) {
  return request('DELETE', `/owners/${id}`, null, { endpoint: '/owners/{id}', method: 'DELETE' });
}

// Scenario configuration ----------------------------------------
function scenarioOptions(name) {
  const thresholds = {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<400'],
  };

  const common = { executor: 'ramping-arrival-rate', preAllocatedVUs: 20, startRate: 1, timeUnit: '1s' };

  const scenarios = {
    spike: {
      ...common,
      stages: [
        { target: 1, duration: '10s' },
        { target: 80, duration: '30s' }, // spike
        { target: 80, duration: '60s' },
        { target: 0, duration: '20s' },
      ],
    },
    ramp: {
      ...common,
      stages: [
        { target: 10, duration: '1m' },
        { target: 30, duration: '2m' },
        { target: 50, duration: '2m' },
        { target: 0, duration: '30s' },
      ],
    },
    soak: {
      executor: 'constant-arrival-rate',
      rate: 20, // per second
      timeUnit: '1s',
      duration: '30m',
      preAllocatedVUs: 30,
      maxVUs: 100,
    },
    stress: {
      ...common,
      stages: [
        { target: 20, duration: '1m' },
        { target: 60, duration: '1m' },
        { target: 120, duration: '1m' },
        { target: 200, duration: '2m' },
        { target: 0, duration: '1m' },
      ],
      preAllocatedVUs: 60,
    },
    critical: {
      ...common,
      stages: [
        { target: 20, duration: '2m' },
        { target: 40, duration: '3m' },
        { target: 0, duration: '30s' },
      ],
    },
    cold: {
      executor: 'shared-iterations',
      vus: 1,
      iterations: 10,
      maxDuration: '30m',
    },
  };

  return { scenarios: { owners: { exec: 'default', ...scenarios[name] } }, thresholds };
}

export const options = scenarioOptions(SCENARIO);

// Setup: seed some owners for GET by id / PUT / DELETE
export function setup() {
  const seedCount = 30;
  const seedIds = [];
  for (let i = 0; i < seedCount; i++) {
    const owner = randomOwner({}, i);
    const res = createOwner(owner);
    try {
      const data = res.json();
      if (data && (data.id !== undefined && data.id !== null)) {
        seedIds.push(data.id);
      }
    } catch (_) {}
    // brief pacing to avoid throttling on seed
    sleep(0.2);
  }
  return { baseUrl: BASE_URL, seedIds };
}

function pickActionName() {
  if (SCENARIO === 'critical') {
    // Focus on critical endpoints only: GET list (60%), POST (25%), PUT (15%)
    return pickWeightedAction([
      { name: 'GET_LIST', w: 60 },
      { name: 'POST', w: 25 },
      { name: 'PUT', w: 15 },
    ]);
  }
  // Default mix: 50% GET list, 20% GET by id, 15% POST, 10% PUT, 5% DELETE
  return pickWeightedAction([
    { name: 'GET_LIST', w: 50 },
    { name: 'GET_BY_ID', w: 20 },
    { name: 'POST', w: 15 },
    { name: 'PUT', w: 10 },
    { name: 'DELETE', w: 5 },
  ]);
}

export default function (data) {
  if (SCENARIO === 'cold') {
    // Single isolated call then long pause to encourage cold starts
    getOwners();
    sleep(60);
    return;
  }

  const action = pickActionName();
  // Combine estáveis da semente com IDs criados durante o teste
  const ID_POOL = (data && Array.isArray(data.seedIds)) ? data.seedIds.concat(CREATED_IDS) : CREATED_IDS;
  switch (action) {
    case 'GET_LIST':
      getOwners();
      break;
    case 'GET_BY_ID':
      if (ID_POOL.length > 0) {
        const id = ID_POOL[randInt(0, ID_POOL.length - 1)];
        getOwnerById(id);
      } else {
        getOwners();
      }
      break;
    case 'POST': {
      const owner = randomOwner();
      createOwner(owner);
      break;
    }
    case 'PUT': {
      if (ID_POOL.length > 0) {
        const id = ID_POOL[randInt(0, ID_POOL.length - 1)];
        const owner = randomOwner();
        updateOwner(id, owner);
      } else {
        const res = createOwner(randomOwner());
        try { const data = res.json(); if (data && data.id) updateOwner(data.id, randomOwner()); } catch (_) {}
      }
      break;
    }
    case 'DELETE': {
      if (ID_POOL.length > 0) {
        const idx = randInt(0, ID_POOL.length - 1);
        const id = ID_POOL[idx];
        const res = deleteOwner(id);
        if (res.status >= 200 && res.status < 300) {
          // Remover somente se foi um ID criado neste VU; seedIds vêm de setup (imutáveis aqui)
          const pos = CREATED_IDS.indexOf(id);
          if (pos >= 0) CREATED_IDS.splice(pos, 1);
        }
      } else {
        // nothing to delete, do a GET as fallback
        getOwners();
      }
      break;
    }
  }

  // light pacing
  sleep(0.2);
}

export function handleSummary(data) {
  const runId = `owners-${SCENARIO}-${new Date().toISOString().replace(/[:.]/g, '-')}`;
  const txt = textSummary(data, { indent: ' ', enableColors: true });
  const json = JSON.stringify(data, null, 2);
  return {
    stdout: `\n--- Summary (${runId}) ---\n${txt}\n`,
    [`load_tests/summary-${runId}.txt`]: txt,
    [`load_tests/summary-${runId}.json`]: json,
  };
}
