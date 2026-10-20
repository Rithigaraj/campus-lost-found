/* ===== Campus Lost & Found - frontend application ===== */

/* ---------- API layer ---------- */
const api = {
  async request(path, method = 'GET', body) {
    const opts = { method, headers: { 'Content-Type': 'application/json' } };
    if (body !== undefined) opts.body = JSON.stringify(body);
    const res = await fetch(path, opts);
    if (res.status === 204) return null;
    let data = null;
    try { data = await res.json(); } catch (_) { /* empty body */ }
    if (!res.ok) {
      const msg = data && (data.error || data.message)
        ? (data.error || data.message)
        : `Request failed (HTTP ${res.status})`;
      throw new Error(msg);
    }
    return data;
  },
  get(path) { return this.request(path); },
  post(path, body) { return this.request(path, 'POST', body); },
  put(path, body) { return this.request(path, 'PUT', body); },
  patch(path, body) { return this.request(path, 'PATCH', body); },
  del(path) { return this.request(path, 'DELETE'); }
};

/* ---------- Helpers ---------- */
function esc(value) {
  return String(value ?? '').replace(/[&<>"']/g, c =>
    ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
}

function badge(status) {
  const s = (status || 'OPEN').toUpperCase();
  return `<span class="badge badge-${s.toLowerCase()}">${esc(s)}</span>`;
}

function scoreSpan(score) {
  return `<span class="score ${score >= 70 ? 'score-hi' : 'score-mid'}">${score}/100</span>`;
}

let toastTimer = null;
function toast(message, type = 'success') {
  const el = document.getElementById('toast');
  el.textContent = message;
  el.className = `toast show ${type}`;
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => el.classList.remove('show'), 3500);
}

function today() {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}

function confirmBox(message) {
  return window.confirm(message);
}

/* ---------- Navigation ---------- */
const viewMeta = {
  'report-lost':  ['Report Lost Item',  'Register an item reported as missing on campus.'],
  'report-found': ['Report Found Item', 'Record the details of an item found on campus.'],
  'reports':      ['View Reports',      'Display all lost and found items stored in the database.'],
  'matching':     ['Smart Matching',    'Compare lost and found reports and calculate a similarity score.'],
  'update':       ['Update Reports',    'Modify item details or update the report status.'],
  'delete':       ['Delete Reports',    'Remove incorrect or unnecessary reports.'],
  'returned':     ['Mark as Returned',  'Update the status when an item is handed back to its owner.']
};

const loaders = {}; // view key -> function called when the view opens

function activateView(key) {
  document.querySelectorAll('.nav-btn').forEach(b =>
    b.classList.toggle('active', b.dataset.view === key));
  document.querySelectorAll('.view').forEach(v =>
    v.classList.toggle('active', v.id === `view-${key}`));
  const [title, subtitle] = viewMeta[key];
  document.getElementById('page-title').textContent = title;
  document.getElementById('page-subtitle').textContent = subtitle;
  if (loaders[key]) loaders[key]();
}

/* ---------- Shared records view (View / Update / Delete / Returned) ---------- */
function createRecordsView(containerId, options = {}) {
  const actions = options.actions || [];
  const root = document.getElementById(containerId);
  const cols = ['ID', 'Item Name', 'Category', 'Location', 'Date', 'Contact', 'Contact Info', 'Status'];
  let side = 'lost';
  let rows = [];

  root.innerHTML = `
    <div class="tabs">
      <button class="tab active" data-side="lost">Lost Items</button>
      <button class="tab" data-side="found">Found Items</button>
      <div style="margin-left:auto; display:flex; align-items:center; gap:12px;">
        <span class="muted" id="${containerId}-count"></span>
        <button class="btn btn-outline btn-sm" id="${containerId}-refresh">Refresh</button>
      </div>
    </div>
    <div class="table-wrap">
      <table>
        <thead><tr>${cols.map(c => `<th>${c}</th>`).join('')}${actions.length ? '<th>Actions</th>' : ''}</tr></thead>
        <tbody id="${containerId}-body"></tbody>
      </table>
      <div class="empty" id="${containerId}-empty">No reports yet.</div>
    </div>`;

  const body = root.querySelector(`#${containerId}-body`);
  const empty = root.querySelector(`#${containerId}-empty`);
  const count = root.querySelector(`#${containerId}-count`);

  async function load() {
    try {
      rows = await api.get(side === 'lost' ? '/api/lost' : '/api/found');
    } catch (err) {
      toast(err.message, 'error');
      return;
    }
    render();
  }

  function render() {
    count.textContent = `${rows.length} report(s)`;
    empty.style.display = rows.length ? 'none' : 'block';
    body.innerHTML = rows.map(r => {
      const id = side === 'lost' ? r.lostId : r.foundId;
      const date = side === 'lost' ? r.dateLost : r.dateFound;
      const cells = [
        id, esc(r.itemName), esc(r.category), esc(r.location || '—'),
        esc(date || '—'), esc(r.contactName), esc(r.contactInfo), badge(r.status)
      ];
      if (actions.length) {
        cells.push(actions.map(a =>
          `<button class="btn btn-sm ${a.cls}" data-action="${a.action}" data-id="${id}">${a.label}</button>`
        ).join(' '));
      }
      return `<tr>${cells.map(c => `<td>${c}</td>`).join('')}</tr>`;
    }).join('');
  }

  root.addEventListener('click', async e => {
    const tab = e.target.closest('.tab');
    if (tab) {
      side = tab.dataset.side;
      root.querySelectorAll('.tab').forEach(t => t.classList.toggle('active', t === tab));
      load();
      return;
    }
    if (e.target.id === `${containerId}-refresh`) { load(); return; }
    const btn = e.target.closest('button[data-action]');
    if (btn) {
      const id = Number(btn.dataset.id);
      const row = rows.find(r => (side === 'lost' ? r.lostId : r.foundId) === id);
      if (row && options.onAction) options.onAction(btn.dataset.action, row, side, load);
    }
  });

  return { load };
}

/* ---------- Edit modal ---------- */
const modal = document.getElementById('edit-modal');
let editState = null; // { side, id }
let updateViewLoad = null;

function openEditModal(side, row) {
  editState = { side, id: side === 'lost' ? row.lostId : row.foundId };
  document.getElementById('edit-title').textContent =
    `Edit ${side === 'lost' ? 'Lost' : 'Found'} Report #${editState.id}`;
  document.getElementById('edit-date-label').textContent =
    side === 'lost' ? 'Date Lost *' : 'Date Found *';
  document.getElementById('edit-itemName').value = row.itemName || '';
  document.getElementById('edit-category').value = row.category || '';
  document.getElementById('edit-description').value = row.description || '';
  document.getElementById('edit-location').value = row.location || '';
  document.getElementById('edit-date').value =
    (side === 'lost' ? row.dateLost : row.dateFound) || '';
  document.getElementById('edit-contactName').value = row.contactName || '';
  document.getElementById('edit-contactInfo').value = row.contactInfo || '';
  document.getElementById('edit-status').value = row.status || 'OPEN';
  modal.classList.add('open');
}

function closeEditModal() {
  modal.classList.remove('open');
  editState = null;
}

async function saveEdit() {
  if (!editState) return;
  const side = editState.side;
  const dateValue = document.getElementById('edit-date').value;
  if (!dateValue) { toast('Date is required', 'error'); return; }
  const payload = {
    itemName: document.getElementById('edit-itemName').value.trim(),
    category: document.getElementById('edit-category').value.trim(),
    description: document.getElementById('edit-description').value.trim(),
    location: document.getElementById('edit-location').value.trim(),
    contactName: document.getElementById('edit-contactName').value.trim(),
    contactInfo: document.getElementById('edit-contactInfo').value.trim(),
    status: document.getElementById('edit-status').value
  };
  payload[side === 'lost' ? 'dateLost' : 'dateFound'] = dateValue;
  if (!payload.itemName || !payload.category || !payload.contactName || !payload.contactInfo) {
    toast('Please fill in all required fields', 'error');
    return;
  }
  try {
    const updatedId = editState.id;
    await api.put(`/api/${side}/${updatedId}`, payload);
    closeEditModal();
    toast(`Report #${updatedId} updated`);
    if (updateViewLoad) updateViewLoad();
  } catch (err) {
    toast(err.message, 'error');
  }
}

/* ---------- Smart matching ---------- */
let matchResults = [];

async function runMatching() {
  const scope = document.getElementById('match-scope').value;
  const idInput = document.getElementById('match-id');
  const id = idInput.value.trim();
  if (scope !== 'all' && !id) {
    toast('Enter the report ID to match', 'error');
    return;
  }
  const query = `/api/matches?scope=${scope}` + (scope !== 'all' ? `&id=${id}` : '');
  try {
    matchResults = await api.get(query);
  } catch (err) {
    toast(err.message, 'error');
    return;
  }
  renderMatches();
}

function renderMatches() {
  const body = document.getElementById('match-body');
  const empty = document.getElementById('match-empty');
  const count = document.getElementById('match-count');
  count.textContent = matchResults.length
    ? `${matchResults.length} possible match(es) — threshold 40/100`
    : '';
  empty.style.display = matchResults.length ? 'none' : 'block';
  if (!matchResults.length) {
    body.innerHTML = '';
    if (matchResults) empty.textContent = 'No matches at or above the similarity threshold (40/100).';
    return;
  }
  body.innerHTML = matchResults.map((m, i) => `
    <tr>
      <td>${scoreSpan(m.score)}</td>
      <td>#${m.lostItem.lostId}</td>
      <td>${esc(m.lostItem.itemName)}</td>
      <td>${esc(m.lostItem.location || '—')}</td>
      <td>${esc(m.lostItem.dateLost || '—')}</td>
      <td>#${m.foundItem.foundId}</td>
      <td>${esc(m.foundItem.itemName)}</td>
      <td>${esc(m.foundItem.location || '—')}</td>
      <td>${esc(m.foundItem.dateFound || '—')}</td>
      <td><button class="btn btn-success btn-sm" data-confirm="${i}">Confirm Match</button></td>
    </tr>`).join('');
}

async function confirmMatch(lostId, foundId, after) {
  try {
    await api.post('/api/matches/confirm', { lostId, foundId });
    toast(`Lost #${lostId} and Found #${foundId} marked as MATCHED`);
    if (after) after();
  } catch (err) {
    toast(err.message, 'error');
  }
}

/* ---------- Suggest box after creating a report ---------- */
async function suggestMatches(side, id, boxId) {
  const box = document.getElementById(boxId);
  let matches;
  try {
    matches = await api.get(`/api/matches?scope=${side}&id=${id}`);
  } catch (_) {
    box.classList.add('hidden');
    return;
  }
  if (!matches.length) {
    box.classList.add('hidden');
    return;
  }
  box.innerHTML = `
    <div class="suggest-title">Smart matching found ${matches.length} possible match(es) for report #${id}:</div>
    ${matches.map(m => `
      <div class="suggest-row">
        <span>${scoreSpan(m.score)} &nbsp; ${esc(m.lostItem.itemName)} (LOST #${m.lostItem.lostId})
          &harr; ${esc(m.foundItem.itemName)} (FOUND #${m.foundItem.foundId})</span>
        <button class="btn btn-success btn-sm" data-lost="${m.lostItem.lostId}" data-found="${m.foundItem.foundId}">Confirm Match</button>
      </div>`).join('')}`;
  box.classList.remove('hidden');
  box.querySelectorAll('button[data-lost]').forEach(btn => {
    btn.addEventListener('click', () =>
      confirmMatch(Number(btn.dataset.lost), Number(btn.dataset.found),
        () => suggestMatches(side, id, boxId)));
  });
}

/* ---------- Report forms ---------- */
async function submitReport(side) {
  const prefix = side;
  const dateKey = side === 'lost' ? 'dateLost' : 'dateFound';
  const payload = {
    itemName: document.getElementById(`${prefix}-itemName`).value.trim(),
    category: document.getElementById(`${prefix}-category`).value.trim(),
    description: document.getElementById(`${prefix}-description`).value.trim(),
    location: document.getElementById(`${prefix}-location`).value.trim(),
    contactName: document.getElementById(`${prefix}-contactName`).value.trim(),
    contactInfo: document.getElementById(`${prefix}-contactInfo`).value.trim(),
    status: 'OPEN'
  };
  payload[dateKey] = document.getElementById(`${prefix}-${dateKey}`).value;
  if (!payload.itemName || !payload.category || !payload[dateKey]
      || !payload.contactName || !payload.contactInfo) {
    toast('Please fill in all required fields', 'error');
    return;
  }
  try {
    const created = await api.post(`/api/${side}`, payload);
    const newId = side === 'lost' ? created.lostId : created.foundId;
    toast(`${side === 'lost' ? 'Lost' : 'Found'} report #${newId} saved successfully`);
    document.getElementById(`${prefix}-form`).reset();
    document.getElementById(`${prefix}-${dateKey}`).value = today();
    suggestMatches(side, newId, `${prefix}-suggest`);
  } catch (err) {
    toast(err.message, 'error');
  }
}

/* ---------- Init ---------- */
document.addEventListener('DOMContentLoaded', () => {

  /* Nav buttons */
  document.querySelectorAll('.nav-btn').forEach(btn =>
    btn.addEventListener('click', () => activateView(btn.dataset.view)));

  /* Records views */
  const viewReports = createRecordsView('records-reports', { actions: [] });

  const viewUpdate = createRecordsView('records-update', {
    actions: [{ label: 'Edit', cls: 'btn-primary', action: 'edit' }],
    onAction: (action, row, side, reload) => {
      if (action === 'edit') openEditModal(side, row);
    }
  });
  updateViewLoad = () => viewUpdate.load();

  const viewDelete = createRecordsView('records-delete', {
    actions: [{ label: 'Delete', cls: 'btn-danger', action: 'delete' }],
    onAction: async (action, row, side, reload) => {
      if (action !== 'delete') return;
      const id = side === 'lost' ? row.lostId : row.foundId;
      if (!confirmBox(`Delete ${side} report #${id} "${row.itemName}"? This cannot be undone.`)) return;
      try {
        await api.del(`/api/${side}/${id}`);
        toast(`Report #${id} deleted`);
        reload();
      } catch (err) { toast(err.message, 'error'); }
    }
  });

  const viewReturned = createRecordsView('records-returned', {
    actions: [{ label: 'Mark Returned', cls: 'btn-success', action: 'return' }],
    onAction: async (action, row, side, reload) => {
      if (action !== 'return') return;
      const id = side === 'lost' ? row.lostId : row.foundId;
      if (!confirmBox(`Mark ${side} report #${id} "${row.itemName}" as RETURNED?`)) return;
      try {
        await api.patch(`/api/${side}/${id}/status`, { status: 'RETURNED' });
        toast(`Report #${id} marked as RETURNED`);
        reload();
      } catch (err) { toast(err.message, 'error'); }
    }
  });

  loaders['reports'] = () => viewReports.load();
  loaders['update'] = () => viewUpdate.load();
  loaders['delete'] = () => viewDelete.load();
  loaders['returned'] = () => viewReturned.load();

  /* Forms */
  document.getElementById('lost-form').addEventListener('submit', e => {
    e.preventDefault(); submitReport('lost');
  });
  document.getElementById('found-form').addEventListener('submit', e => {
    e.preventDefault(); submitReport('found');
  });
  document.getElementById('lost-dateLost').value = today();
  document.getElementById('found-dateFound').value = today();

  /* Smart matching controls */
  document.getElementById('match-scope').addEventListener('change', e => {
    document.getElementById('match-id').classList.toggle('hidden', e.target.value === 'all');
  });
  document.getElementById('match-run').addEventListener('click', runMatching);
  document.getElementById('match-body').addEventListener('click', e => {
    const btn = e.target.closest('button[data-confirm]');
    if (!btn) return;
    const m = matchResults[Number(btn.dataset.confirm)];
    if (m) confirmMatch(m.lostItem.lostId, m.foundItem.foundId, runMatching);
  });

  /* Edit modal */
  document.getElementById('edit-save').addEventListener('click', saveEdit);
  document.getElementById('edit-cancel').addEventListener('click', closeEditModal);
  document.getElementById('edit-close').addEventListener('click', closeEditModal);
  modal.addEventListener('click', e => { if (e.target === modal) closeEditModal(); });

  /* Database badge probe */
  api.get('/api/lost').then(() => {
    document.getElementById('db-badge-text').textContent = 'MySQL connected';
  }).catch(err => {
    const badgeEl = document.getElementById('db-badge');
    badgeEl.classList.add('error');
    document.getElementById('db-badge-text').textContent = 'DB error';
    toast(err.message, 'error');
  });
});
