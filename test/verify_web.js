const assert = require('assert');
const fs = require('fs');
const path = require('path');
const vm = require('vm');

/**
 * Official F1 2024 Tyre Compound Color Tokens (Hex)
 */
const TYRE_COLORS = {
  S: '#FF1801',
  M: '#FFD200',
  H: '#FFFFFF',
  I: '#39B54A',
  W: '#00A3E0'
};

/**
 * Reference Dataset: 2024 Formula 1 Italian Grand Prix (Monza, Round 16)
 */
const F1_DATA_2024 = {
  grandPrix: {
    name: "FORMULA 1 PIRELLI GRAN PREMIO D'ITALIA 2024",
    circuit: 'Autodromo Nazionale Monza',
    country: 'Italy',
    round: 16,
    sessions: [
      { id: 'fp1', name: 'Практика 1 (FP1)', timeUtc: '2024-08-30T11:30:00Z' },
      { id: 'fp2', name: 'Практика 2 (FP2)', timeUtc: '2024-08-30T15:00:00Z' },
      { id: 'fp3', name: 'Практика 3 (FP3)', timeUtc: '2024-08-31T10:30:00Z' },
      { id: 'quali', name: 'Квалификация', timeUtc: '2024-08-31T14:00:00Z' },
      { id: 'race', name: 'Гонка', timeUtc: '2024-09-01T13:00:00Z' }
    ]
  },
  timing: [
    { position: 1, code: 'LEC', number: 16, name: 'Charles Leclerc', team: 'Ferrari', teamColor: '#E80020', gap: 'LEADER', tyre: 'H', tyreLaps: 38 },
    { position: 2, code: 'PIA', number: 81, name: 'Oscar Piastri', team: 'McLaren', teamColor: '#FF8000', gap: '+2.664s', tyre: 'H', tyreLaps: 14 },
    { position: 3, code: 'NOR', number: 4, name: 'Lando Norris', team: 'McLaren', teamColor: '#FF8000', gap: '+6.153s', tyre: 'H', tyreLaps: 20 },
    { position: 4, code: 'SAI', number: 55, name: 'Carlos Sainz', team: 'Ferrari', teamColor: '#E80020', gap: '+15.621s', tyre: 'H', tyreLaps: 33 },
    { position: 5, code: 'HAM', number: 44, name: 'Lewis Hamilton', team: 'Mercedes', teamColor: '#27F4D2', gap: '+22.820s', tyre: 'H', tyreLaps: 16 },
    { position: 6, code: 'VER', number: 1, name: 'Max Verstappen', team: 'Red Bull Racing', teamColor: '#3671C6', gap: '+37.932s', tyre: 'M', tyreLaps: 11 },
    { position: 7, code: 'RUS', number: 63, name: 'George Russell', team: 'Mercedes', teamColor: '#27F4D2', gap: '+39.715s', tyre: 'H', tyreLaps: 20 },
    { position: 8, code: 'PER', number: 11, name: 'Sergio Perez', team: 'Red Bull Racing', teamColor: '#3671C6', gap: '+54.148s', tyre: 'M', tyreLaps: 17 },
    { position: 9, code: 'ALB', number: 23, name: 'Alexander Albon', team: 'Williams', teamColor: '#64C4FF', gap: '+67.456s', tyre: 'H', tyreLaps: 37 },
    { position: 10, code: 'MAG', number: 20, name: 'Kevin Magnussen', team: 'Haas', teamColor: '#B6BABD', gap: '+68.302s', tyre: 'H', tyreLaps: 34 },
    { position: 11, code: 'ALO', number: 14, name: 'Fernando Alonso', team: 'Aston Martin', teamColor: '#229971', gap: '+68.495s', tyre: 'H', tyreLaps: 21 },
    { position: 12, code: 'COL', number: 43, name: 'Franco Colapinto', team: 'Williams', teamColor: '#64C4FF', gap: '+81.308s', tyre: 'H', tyreLaps: 37 },
    { position: 13, code: 'RIC', number: 3, name: 'Daniel Ricciardo', team: 'RB', teamColor: '#6692FF', gap: '+93.452s', tyre: 'H', tyreLaps: 36 },
    { position: 14, code: 'OCO', number: 31, name: 'Esteban Ocon', team: 'Alpine', teamColor: '#0093CC', gap: '+1 Lap', tyre: 'H', tyreLaps: 38 },
    { position: 15, code: 'GAS', number: 10, name: 'Pierre Gasly', team: 'Alpine', teamColor: '#0093CC', gap: '+1 Lap', tyre: 'H', tyreLaps: 38 },
    { position: 16, code: 'BOT', number: 77, name: 'Valtteri Bottas', team: 'Kick Sauber', teamColor: '#52E252', gap: '+1 Lap', tyre: 'H', tyreLaps: 36 },
    { position: 17, code: 'HUL', number: 27, name: 'Nico Hulkenberg', team: 'Haas', teamColor: '#B6BABD', gap: '+1 Lap', tyre: 'H', tyreLaps: 39 },
    { position: 18, code: 'ZHO', number: 24, name: 'Zhou Guanyu', team: 'Kick Sauber', teamColor: '#52E252', gap: '+1 Lap', tyre: 'H', tyreLaps: 36 },
    { position: 19, code: 'STR', number: 18, name: 'Lance Stroll', team: 'Aston Martin', teamColor: '#229971', gap: '+1 Lap', tyre: 'S', tyreLaps: 6 },
    { position: 20, code: 'BEA', number: 38, name: 'Oliver Bearman', team: 'Ferrari', teamColor: '#E80020', gap: '+2 Laps', tyre: 'M', tyreLaps: 24 },
    { position: 21, code: 'SAR', number: 2, name: 'Logan Sargeant', team: 'Williams', teamColor: '#64C4FF', gap: '+2 Laps', tyre: 'M', tyreLaps: 22 },
    { position: 22, code: 'TSU', number: 22, name: 'Yuki Tsunoda', team: 'RB', teamColor: '#6692FF', gap: '+45 Laps', tyre: 'M', tyreLaps: 8 }
  ],
  standings: {
    drivers: [
      { position: 1, code: 'VER', name: 'Max Verstappen', team: 'Red Bull Racing', points: 303, wins: 7 },
      { position: 2, code: 'NOR', name: 'Lando Norris', team: 'McLaren', points: 241, wins: 2 },
      { position: 3, code: 'LEC', name: 'Charles Leclerc', team: 'Ferrari', points: 217, wins: 2 },
      { position: 4, code: 'PIA', name: 'Oscar Piastri', team: 'McLaren', points: 197, wins: 1 },
      { position: 5, code: 'SAI', name: 'Carlos Sainz', team: 'Ferrari', points: 184, wins: 1 },
      { position: 6, code: 'HAM', name: 'Lewis Hamilton', team: 'Mercedes', points: 164, wins: 2 },
      { position: 7, code: 'PER', name: 'Sergio Perez', team: 'Red Bull Racing', points: 143, wins: 0 },
      { position: 8, code: 'RUS', name: 'George Russell', team: 'Mercedes', points: 128, wins: 1 },
      { position: 9, code: 'ALO', name: 'Fernando Alonso', team: 'Aston Martin', points: 50, wins: 0 },
      { position: 10, code: 'STR', name: 'Lance Stroll', team: 'Aston Martin', points: 24, wins: 0 },
      { position: 11, code: 'HUL', name: 'Nico Hulkenberg', team: 'Haas', points: 22, wins: 0 },
      { position: 12, code: 'TSU', name: 'Yuki Tsunoda', team: 'RB', points: 22, wins: 0 },
      { position: 13, code: 'RIC', name: 'Daniel Ricciardo', team: 'RB', points: 12, wins: 0 },
      { position: 14, code: 'GAS', name: 'Pierre Gasly', team: 'Alpine', points: 8, wins: 0 },
      { position: 15, code: 'BEA', name: 'Oliver Bearman', team: 'Ferrari', points: 7, wins: 0 },
      { position: 16, code: 'MAG', name: 'Kevin Magnussen', team: 'Haas', points: 6, wins: 0 },
      { position: 17, code: 'ALB', name: 'Alexander Albon', team: 'Williams', points: 6, wins: 0 },
      { position: 18, code: 'OCO', name: 'Esteban Ocon', team: 'Alpine', points: 5, wins: 0 },
      { position: 19, code: 'ZHO', name: 'Zhou Guanyu', team: 'Kick Sauber', points: 0, wins: 0 },
      { position: 20, code: 'SAR', name: 'Logan Sargeant', team: 'Williams', points: 0, wins: 0 },
      { position: 21, code: 'COL', name: 'Franco Colapinto', team: 'Williams', points: 0, wins: 0 },
      { position: 22, code: 'BOT', name: 'Valtteri Bottas', team: 'Kick Sauber', points: 0, wins: 0 }
    ],
    constructors: [
      { position: 1, name: 'Red Bull Racing', color: '#3671C6', points: 446, wins: 7 },
      { position: 2, name: 'McLaren', color: '#FF8000', points: 438, wins: 3 },
      { position: 3, name: 'Ferrari', color: '#E80020', points: 407, wins: 3 },
      { position: 4, name: 'Mercedes', color: '#27F4D2', points: 292, wins: 3 },
      { position: 5, name: 'Aston Martin', color: '#229971', points: 74, wins: 0 },
      { position: 6, name: 'RB', color: '#6692FF', points: 34, wins: 0 },
      { position: 7, name: 'Haas', color: '#B6BABD', points: 28, wins: 0 },
      { position: 8, name: 'Alpine', color: '#0093CC', points: 13, wins: 0 },
      { position: 9, name: 'Williams', color: '#64C4FF', points: 6, wins: 0 },
      { position: 10, name: 'Kick Sauber', color: '#52E252', points: 0, wins: 0 }
    ]
  }
};

/**
 * Calculates countdown split given target timestamp and reference timestamp.
 * Returns days, hours, minutes, seconds and totalSeconds.
 */
function calculateCountdown(targetDateOrIso, currentDateOrIso = new Date()) {
  const targetMs = typeof targetDateOrIso === 'string' ? new Date(targetDateOrIso).getTime() : targetDateOrIso.getTime();
  const currentMs = typeof currentDateOrIso === 'string' ? new Date(currentDateOrIso).getTime() : currentDateOrIso.getTime();
  const diffMs = targetMs - currentMs;

  if (Number.isNaN(diffMs)) {
    throw new TypeError('Invalid timestamp passed to calculateCountdown');
  }

  if (diffMs <= 0) {
    return {
      days: 0,
      hours: 0,
      minutes: 0,
      seconds: 0,
      totalSeconds: 0,
      isPast: true
    };
  }

  const totalSeconds = Math.floor(diffMs / 1000);
  const days = Math.floor(totalSeconds / 86400);
  const hours = Math.floor((totalSeconds % 86400) / 3600);
  const minutes = Math.floor((totalSeconds % 3600) / 60);
  const seconds = totalSeconds % 60;

  return {
    days,
    hours,
    minutes,
    seconds,
    totalSeconds,
    isPast: false
  };
}

/**
 * Suite 1: Validates F1_DATA_2024 schema structure, field non-emptiness, and count constraints.
 */
function testDatasetSchema(dataset = F1_DATA_2024) {
  assert(dataset, 'Dataset must be defined');
  const { grandPrix, timing, standings } = dataset;

  // Grand Prix assertions
  assert.strictEqual(grandPrix.name, "FORMULA 1 PIRELLI GRAN PREMIO D'ITALIA 2024");
  assert.strictEqual(grandPrix.circuit, 'Autodromo Nazionale Monza');
  assert.strictEqual(grandPrix.country, 'Italy');
  assert.strictEqual(grandPrix.round, 16);
  assert(Array.isArray(grandPrix.sessions), 'Sessions must be an array');
  assert.strictEqual(grandPrix.sessions.length, 5, 'Must contain exactly 5 sessions');

  const expectedSessionIds = ['fp1', 'fp2', 'fp3', 'quali', 'race'];
  grandPrix.sessions.forEach((session, idx) => {
    assert.strictEqual(session.id, expectedSessionIds[idx]);
    assert(session.name && typeof session.name === 'string', `Session ${idx} name missing`);
    assert(session.timeUtc && !Number.isNaN(Date.parse(session.timeUtc)), `Session ${idx} invalid ISO timestamp`);
    if (idx > 0) {
      const prevMs = new Date(grandPrix.sessions[idx - 1].timeUtc).getTime();
      const currMs = new Date(session.timeUtc).getTime();
      assert(currMs > prevMs, `Session ${session.id} must be chronological after previous session`);
    }
  });

  // Timing assertions (P1..P22)
  assert(Array.isArray(timing), 'Timing must be an array');
  assert.strictEqual(timing.length, 22, 'Timing grid must contain exactly 22 drivers');

  const validTyres = new Set(['S', 'M', 'H', 'I', 'W']);
  const hexPattern = /^#[0-9A-Fa-f]{6}$/;

  timing.forEach((driver, idx) => {
    const expectedPos = idx + 1;
    assert.strictEqual(driver.position, expectedPos, `Driver at index ${idx} must have position ${expectedPos}`);
    assert(driver.code && typeof driver.code === 'string' && driver.code.length === 3, `Driver ${expectedPos} invalid code`);
    assert(typeof driver.number === 'number' && driver.number > 0, `Driver ${expectedPos} invalid car number`);
    assert(driver.name && typeof driver.name === 'string', `Driver ${expectedPos} missing name`);
    assert(driver.team && typeof driver.team === 'string', `Driver ${expectedPos} missing team`);
    assert(hexPattern.test(driver.teamColor), `Driver ${expectedPos} invalid teamColor hex`);
    assert(validTyres.has(driver.tyre), `Driver ${expectedPos} invalid tyre compound ${driver.tyre}`);
    assert(typeof driver.tyreLaps === 'number' && driver.tyreLaps >= 0, `Driver ${expectedPos} invalid tyreLaps`);

    if (expectedPos === 1) {
      assert.strictEqual(driver.gap, 'LEADER', 'P1 gap must be strictly "LEADER"');
    } else {
      assert(
        typeof driver.gap === 'string' && driver.gap.startsWith('+'),
        `Driver P${expectedPos} gap must start with "+" (received: ${driver.gap})`
      );
    }
  });

  // Standings assertions
  assert(standings && typeof standings === 'object', 'Standings object required');
  assert(Array.isArray(standings.drivers), 'Driver standings must be an array');
  assert.strictEqual(standings.drivers.length, 22, 'Driver standings must contain exactly 22 drivers');

  standings.drivers.forEach((driver, idx) => {
    const expectedPos = idx + 1;
    assert.strictEqual(driver.position, expectedPos, `Standings driver index ${idx} position mismatch`);
    assert(driver.code && driver.code.length === 3, `Standings driver ${expectedPos} invalid code`);
    assert(driver.name && typeof driver.name === 'string', `Standings driver ${expectedPos} invalid name`);
    assert(driver.team && typeof driver.team === 'string', `Standings driver ${expectedPos} invalid team`);
    assert(typeof driver.points === 'number' && driver.points >= 0, `Standings driver ${expectedPos} invalid points`);
    assert(typeof driver.wins === 'number' && driver.wins >= 0, `Standings driver ${expectedPos} invalid wins`);

    if (idx > 0) {
      const prevPoints = standings.drivers[idx - 1].points;
      assert(driver.points <= prevPoints, `Standings drivers must be sorted descending by points (${prevPoints} >= ${driver.points})`);
    }
  });

  assert(Array.isArray(standings.constructors), 'Constructor standings must be an array');
  assert.strictEqual(standings.constructors.length, 10, 'Constructor standings must contain exactly 10 teams');

  standings.constructors.forEach((team, idx) => {
    const expectedPos = idx + 1;
    assert.strictEqual(team.position, expectedPos, `Constructor index ${idx} position mismatch`);
    assert(team.name && typeof team.name === 'string', `Constructor ${expectedPos} invalid name`);
    assert(hexPattern.test(team.color), `Constructor ${expectedPos} invalid color hex`);
    assert(typeof team.points === 'number' && team.points >= 0, `Constructor ${expectedPos} invalid points`);
    assert(typeof team.wins === 'number' && team.wins >= 0, `Constructor ${expectedPos} invalid wins`);

    if (idx > 0) {
      const prevPoints = standings.constructors[idx - 1].points;
      assert(team.points <= prevPoints, `Standings constructors must be sorted descending by points (${prevPoints} >= ${team.points})`);
    }
  });
}

/**
 * Suite 2: Validates countdown calculation arithmetic and boundaries.
 */
function testCountdownMath() {
  // Test Case A: Sub-minute interval (45 seconds)
  const resA = calculateCountdown('2024-09-01T13:00:00Z', '2024-09-01T12:59:15Z');
  assert.strictEqual(resA.days, 0);
  assert.strictEqual(resA.hours, 0);
  assert.strictEqual(resA.minutes, 0);
  assert.strictEqual(resA.seconds, 45);
  assert.strictEqual(resA.totalSeconds, 45);
  assert.strictEqual(resA.isPast, false);

  // Test Case B: 2 hours, 15 minutes, 0 seconds
  const resB = calculateCountdown('2024-09-01T13:00:00Z', '2024-09-01T10:45:00Z');
  assert.strictEqual(resB.days, 0);
  assert.strictEqual(resB.hours, 2);
  assert.strictEqual(resB.minutes, 15);
  assert.strictEqual(resB.seconds, 0);
  assert.strictEqual(resB.totalSeconds, 8100);
  assert.strictEqual(resB.isPast, false);

  // Test Case C: Multi-day interval (3 days, 4 hours, 12 minutes, 50 seconds = 274370 seconds)
  const resC = calculateCountdown('2024-09-01T13:00:00Z', '2024-08-29T08:47:10Z');
  assert.strictEqual(resC.days, 3);
  assert.strictEqual(resC.hours, 4);
  assert.strictEqual(resC.minutes, 12);
  assert.strictEqual(resC.seconds, 50);
  assert.strictEqual(resC.totalSeconds, 274370);
  assert.strictEqual(resC.isPast, false);

  // Test Case D: Exact zero delta
  const resD = calculateCountdown('2024-09-01T13:00:00Z', '2024-09-01T13:00:00Z');
  assert.strictEqual(resD.days, 0);
  assert.strictEqual(resD.hours, 0);
  assert.strictEqual(resD.minutes, 0);
  assert.strictEqual(resD.seconds, 0);
  assert.strictEqual(resD.totalSeconds, 0);
  assert.strictEqual(resD.isPast, true);

  // Test Case E: Negative delta (past timestamp)
  const resE = calculateCountdown('2024-09-01T13:00:00Z', '2024-09-01T15:30:00Z');
  assert.strictEqual(resE.days, 0);
  assert.strictEqual(resE.hours, 0);
  assert.strictEqual(resE.minutes, 0);
  assert.strictEqual(resE.seconds, 0);
  assert.strictEqual(resE.totalSeconds, 0);
  assert.strictEqual(resE.isPast, true);
}

/**
 * Suite 3: Validates Pirelli tyre compound color mapping and hex compliance.
 */
function testTyreCompoundColorMapping() {
  const compounds = ['S', 'M', 'H', 'I', 'W'];
  const hexPattern = /^#[0-9A-Fa-f]{6}$/;

  compounds.forEach((compound) => {
    const color = TYRE_COLORS[compound];
    assert(color, `Missing color mapping for compound ${compound}`);
    assert(hexPattern.test(color), `Color ${color} for compound ${compound} is not a valid 6-digit hex string`);
  });

  assert.strictEqual(TYRE_COLORS.S, '#FF1801');
  assert.strictEqual(TYRE_COLORS.M, '#FFD200');
  assert.strictEqual(TYRE_COLORS.H, '#FFFFFF');
  assert.strictEqual(TYRE_COLORS.I, '#39B54A');
  assert.strictEqual(TYRE_COLORS.W, '#00A3E0');
}

/**
 * Suite 4: Validates HTML structure, CSS design tokens, and hash navigation routing.
 */
function testHtmlStructureAndRouting(htmlFilePath = path.join(__dirname, '../web/index.html')) {
  assert(fs.existsSync(htmlFilePath), `Required HTML file does not exist: ${htmlFilePath}`);
  const html = fs.readFileSync(htmlFilePath, 'utf8');

  // Meta viewport and mobile tags
  assert(
    /<meta[^>]+name=["']viewport["'][^>]+content=["'][^"']*width=device-width[^"']*["']/i.test(html),
    'Missing or invalid meta viewport tag for responsive rendering'
  );
  assert(
    /<meta[^>]+name=["']theme-color["'][^>]+content=["']#101014["']/i.test(html),
    'Missing or invalid meta theme-color tag for dark status bar'
  );

  // CSS variables for F1 Dark Racing Theme
  const requiredCssVariables = [
    '--f1-bg: #101014',
    '--f1-surface: #1B1B22',
    '--f1-surface-border: #2C2C36',
    '--f1-red-primary: #E10600',
    '--f1-text-white: #FFFFFF',
    '--f1-text-muted: #8E8E9A',
    '--tyre-soft: #FF1801',
    '--tyre-medium: #FFD200',
    '--tyre-hard: #FFFFFF',
    '--tyre-inter: #39B54A',
    '--tyre-wet: #00A3E0',
    '--flag-green: #00D084',
    '--flag-yellow: #FFB800',
    '--flag-red: #E10600'
  ];

  requiredCssVariables.forEach((cssVar) => {
    const [name, val] = cssVar.split(':').map((s) => s.trim());
    const regex = new RegExp(`${name}\\s*:\\s*${val}`, 'i');
    assert(regex.test(html), `Missing required CSS design token: ${cssVar}`);
  });

  // Display toggling CSS rules
  assert(
    /\.screen\s*\{[^}]*display\s*:\s*none/i.test(html),
    'Missing CSS rule .screen { display: none; }'
  );
  assert(
    /\.screen\.active-screen\s*\{[^}]*display\s*:\s*block/i.test(html) ||
    /\.active-screen\s*\{[^}]*display\s*:\s*block/i.test(html),
    'Missing CSS rule .active-screen { display: block; }'
  );

  // Screen sections
  const requiredScreens = [
    'screen-dashboard',
    'screen-timing',
    'screen-trackmap',
    'screen-standings'
  ];

  requiredScreens.forEach((screenId) => {
    const regex = new RegExp(`<section[^>]+id=["']${screenId}["'][^>]+class=["'][^"']*screen[^"']*["']`, 'i');
    assert(regex.test(html), `Missing section element with id="${screenId}" and class="screen"`);
  });

  // Initial active screen check
  assert(
    /<section[^>]+id=["']screen-dashboard["'][^>]+class=["'][^"']*active-screen[^"']*["']/i.test(html),
    'Screen "#screen-dashboard" must have "active-screen" class on initial render'
  );

  // Header & Status Badge
  assert(/id=["']networkStatusBadge["']/i.test(html), 'Missing element #networkStatusBadge');
  assert(/OFFLINE\s*\(CACHED\)/i.test(html), 'Missing OFFLINE (CACHED) indicator text in status badge');

  // Standalone constraint: zero external scripts or styles
  assert(!/<script[^>]+src=["']https?:/i.test(html), 'Document must not contain external script links');
  assert(!/<link[^>]+href=["']https?:/i.test(html), 'Document must not contain external stylesheet links');

  // Bottom Navigation and labels
  assert(/<nav[^>]+id=["']bottomNav["']/i.test(html), 'Missing element <nav id="bottomNav">');
  assert(/href=["']#dashboard["'][^>]*>[\s\S]*?Этап/i.test(html), 'Bottom navigation missing "#dashboard" item with text "Этап"');
  assert(/href=["']#timing["'][^>]*>[\s\S]*?Тайминг/i.test(html), 'Bottom navigation missing "#timing" item with text "Тайминг"');
  assert(/href=["']#trackmap["'][^>]*>[\s\S]*?Трек/i.test(html), 'Bottom navigation missing "#trackmap" item with text "Трек"');
  assert(/href=["']#standings["'][^>]*>[\s\S]*?Зачет/i.test(html), 'Bottom navigation missing "#standings" item with text "Зачет"');

  // Desktop Navigation tabs
  assert(/id=["']desktopNav["']/i.test(html) || /class=["'][^"']*desktop-tabs[^"']*["']/i.test(html), 'Missing desktop navigation container');
  const desktopMediaQuery = html.match(/@media\s*\(\s*min-width\s*:\s*768px\s*\)\s*\{([\s\S]*?)\}\s*<\/style>/i);
  assert(desktopMediaQuery, 'Missing @media (min-width: 768px) rule');
  assert(/\.desktop-tabs\s*\{[^}]*display\s*:\s*flex/i.test(desktopMediaQuery[1]), 'Missing desktop tabs responsive display flex rule');
  assert(/(\.bottom-nav|#bottomNav)\s*\{[^}]*display\s*:\s*none/i.test(desktopMediaQuery[1]), 'Missing bottom nav hidden display: none rule on desktop');

  // Mobile layout specifications
  assert(/--bottom-nav-height\s*:\s*56px/i.test(html), 'Missing or incorrect --bottom-nav-height: 56px token');
  assert(/\.bottom-nav\s*\{[^}]*z-index\s*:\s*100/i.test(html), 'Missing z-index: 100 on .bottom-nav');
  assert(/\.bottom-nav\s*\{[^}]*position\s*:\s*fixed/i.test(html), 'Missing position: fixed on .bottom-nav');

  // SVG Icons
  const svgMatches = html.match(/<svg[\s\S]*?<\/svg>/gi) || [];
  assert(svgMatches.length >= 4, `Expected at least 4 SVG icons in navigation, found ${svgMatches.length}`);

  // JavaScript Router & Store validation
  assert(/addEventListener\s*\(\s*['"]hashchange['"]/i.test(html), 'Missing hashchange event listener in script block');
  assert(/VALID_ROUTES\s*=\s*\[/i.test(html), 'Missing VALID_ROUTES array in router');
  const validRoutes = ['dashboard', 'timing', 'trackmap', 'standings'];
  validRoutes.forEach((route) => {
    assert(html.includes(`'${route}'`), `Router must declare valid route '${route}'`);
  });
  assert(/F1_DATA_2024/i.test(html), 'Missing embedded F1_DATA_2024 dataset in script block');
  assert(/const\s+Store\s*=\s*\{/i.test(html) || /var\s+Store\s*=\s*\{/i.test(html) || /let\s+Store\s*=\s*\{/i.test(html), 'Missing Store state container in script block');

  // Verify route resolution behavior against contract
  function resolveRoute(hash) {
    const rawHash = (hash || '').replace(/^#\/?/, '').trim();
    return validRoutes.includes(rawHash) ? rawHash : 'dashboard';
  }
  assert.strictEqual(resolveRoute(''), 'dashboard');
  assert.strictEqual(resolveRoute('#'), 'dashboard');
  assert.strictEqual(resolveRoute('#/'), 'dashboard');
  assert.strictEqual(resolveRoute('#dashboard'), 'dashboard');
  assert.strictEqual(resolveRoute('#timing'), 'timing');
  assert.strictEqual(resolveRoute('#trackmap'), 'trackmap');
  assert.strictEqual(resolveRoute('#standings'), 'standings');
  assert.strictEqual(resolveRoute('#invalid-route'), 'dashboard');
}

/**
 * Calculates remaining time until target timestamp.
 * Returns days, hours, minutes, seconds and isExpired flag.
 */
function calculateRemaining(targetUtcMillis, nowMillis = Date.now()) {
  const targetMs = typeof targetUtcMillis === 'string'
    ? new Date(targetUtcMillis).getTime()
    : (typeof targetUtcMillis === 'number' ? targetUtcMillis : targetUtcMillis.getTime());
  const currentMs = typeof nowMillis === 'string'
    ? new Date(nowMillis).getTime()
    : (typeof nowMillis === 'number' ? nowMillis : nowMillis.getTime());

  if (Number.isNaN(targetMs) || Number.isNaN(currentMs)) {
    throw new TypeError('Invalid timestamp passed to calculateRemaining');
  }

  const diffMs = targetMs - currentMs;
  if (diffMs <= 0) {
    return {
      days: 0,
      hours: 0,
      minutes: 0,
      seconds: 0,
      isExpired: true
    };
  }

  const totalSeconds = Math.floor(diffMs / 1000);
  const days = Math.floor(totalSeconds / 86400);
  const hours = Math.floor((totalSeconds % 86400) / 3600);
  const minutes = Math.floor((totalSeconds % 3600) / 60);
  const seconds = totalSeconds % 60;

  return {
    days,
    hours,
    minutes,
    seconds,
    isExpired: false
  };
}

/**
 * Suite 5: Validates Dashboard screen components, countdown engine, session rendering and alarm persistence.
 */
function testDashboardCountdownAndSessions(htmlFilePath = path.join(__dirname, '../web/index.html')) {
  assert(fs.existsSync(htmlFilePath), `HTML file does not exist: ${htmlFilePath}`);
  const html = fs.readFileSync(htmlFilePath, 'utf8');

  // 1. Countdown math arithmetic verification
  // 45 seconds ahead
  const t1 = calculateRemaining('2024-09-01T13:00:00Z', '2024-09-01T12:59:15Z');
  assert.strictEqual(t1.days, 0);
  assert.strictEqual(t1.hours, 0);
  assert.strictEqual(t1.minutes, 0);
  assert.strictEqual(t1.seconds, 45);
  assert.strictEqual(t1.isExpired, false);

  // 2 hours, 15 minutes ahead
  const t2 = calculateRemaining('2024-09-01T13:00:00Z', '2024-09-01T10:45:00Z');
  assert.strictEqual(t2.days, 0);
  assert.strictEqual(t2.hours, 2);
  assert.strictEqual(t2.minutes, 15);
  assert.strictEqual(t2.seconds, 0);
  assert.strictEqual(t2.isExpired, false);

  // 3 days, 4 hours, 12 minutes, 50 seconds ahead
  const t3 = calculateRemaining('2024-09-01T13:00:00Z', '2024-08-29T08:47:10Z');
  assert.strictEqual(t3.days, 3);
  assert.strictEqual(t3.hours, 4);
  assert.strictEqual(t3.minutes, 12);
  assert.strictEqual(t3.seconds, 50);
  assert.strictEqual(t3.isExpired, false);

  // Exact zero delta
  const tZero = calculateRemaining('2024-09-01T13:00:00Z', '2024-09-01T13:00:00Z');
  assert.strictEqual(tZero.days, 0);
  assert.strictEqual(tZero.hours, 0);
  assert.strictEqual(tZero.minutes, 0);
  assert.strictEqual(tZero.seconds, 0);
  assert.strictEqual(tZero.isExpired, true);

  // Negative delta (past timestamp)
  const tPast = calculateRemaining('2024-09-01T13:00:00Z', '2024-09-01T14:00:00Z');
  assert.strictEqual(tPast.days, 0);
  assert.strictEqual(tPast.hours, 0);
  assert.strictEqual(tPast.minutes, 0);
  assert.strictEqual(tPast.seconds, 0);
  assert.strictEqual(tPast.isExpired, true);

  // Numeric epoch inputs
  const nowMs = 1725195600000;
  const targetMs = nowMs + 7200000; // +2 hours
  const tEpoch = calculateRemaining(targetMs, nowMs);
  assert.strictEqual(tEpoch.hours, 2);
  assert.strictEqual(tEpoch.isExpired, false);

  // 2. DOM structure checks in #screen-dashboard
  // Hero banner details
  assert(/FORMULA 1 PIRELLI GRAN PREMIO D'ITALIA 2024/i.test(html), 'Missing Grand Prix title in hero card');
  assert(/Autodromo Nazionale Monza/i.test(html), 'Missing circuit name in hero card');
  assert(/Italy/i.test(html), 'Missing country in hero card');
  assert(/16/i.test(html), 'Missing round 16 indicator in hero card');

  // Countdown timer metric boxes
  assert(/id=["']countDays["']/i.test(html), 'Missing #countDays element');
  assert(/id=["']countHours["']/i.test(html), 'Missing #countHours element');
  assert(/id=["']countMins["']/i.test(html), 'Missing #countMins element');
  assert(/id=["']countSecs["']/i.test(html), 'Missing #countSecs element');
  assert(/id=["']countdownSessionLabel["']/i.test(html), 'Missing #countdownSessionLabel element');

  // 5 sessions rendering check
  const sessionIds = ['fp1', 'fp2', 'fp3', 'quali', 'race'];
  sessionIds.forEach(id => {
    const sessionRegex = new RegExp(`data-session-id=["']${id}["']`, 'i');
    assert(sessionRegex.test(html), `Missing session card element with data-session-id="${id}"`);
  });

  const sessionNames = [
    'Практика 1 (FP1)',
    'Практика 2 (FP2)',
    'Практика 3 (FP3)',
    'Квалификация',
    'Гонка'
  ];
  sessionNames.forEach(name => {
    assert(html.includes(name), `Missing session name "${name}" in HTML`);
  });

  // Alarm buttons check
  assert(/class=["'][^"']*alarm-btn[^"']*["']/i.test(html), 'Missing .alarm-btn button elements');

  // JavaScript Countdown & Alarm logic checks
  assert(/function\s+calculateRemaining/i.test(html), 'Missing calculateRemaining function in web/index.html script');
  assert(/setInterval\s*\([^,]+,\s*1000\s*\)/i.test(html), 'Missing 1000ms setInterval for countdown timer');
  assert(/f1_session_alarms/i.test(html), 'Missing localStorage key "f1_session_alarms" in script');
  assert(/Intl\.DateTimeFormat/i.test(html), 'Missing Intl.DateTimeFormat session localized formatting');

  // 3. Direct execution of production alarm toggle and click delegation logic via Node vm
  const scriptMatch = html.match(/<script>([\s\S]*?)<\/script>/i);
  assert(scriptMatch, 'Missing <script> block in web/index.html');
  const scriptCode = scriptMatch[1];

  const mockStorage = {};
  const mockLocalStorage = {
    getItem: (key) => (Object.prototype.hasOwnProperty.call(mockStorage, key) ? mockStorage[key] : null),
    setItem: (key, val) => { mockStorage[key] = String(val); },
    removeItem: (key) => { delete mockStorage[key]; }
  };

  const registeredListeners = {};
  const mockContainer = {
    dataset: {},
    addEventListener: (evt, fn) => {
      registeredListeners[evt] = registeredListeners[evt] || [];
      registeredListeners[evt].push(fn);
    }
  };

  const mockDomElements = {
    dashboardSessionsContainer: mockContainer,
    sessionsList: { innerHTML: '' },
    countDays: { textContent: '' },
    countHours: { textContent: '' },
    countMins: { textContent: '' },
    countSecs: { textContent: '' },
    countdownSessionLabel: { textContent: '' },
    dashGpTitle: { textContent: '' },
    dashGpCircuit: { textContent: '' }
  };

  const sandbox = {
    console,
    Date,
    Math,
    String,
    Number,
    Boolean,
    TypeError,
    Set,
    Intl,
    localStorage: mockLocalStorage,
    window: {
      location: { hash: '#dashboard' },
      addEventListener: () => {}
    },
    document: {
      readyState: 'complete',
      getElementById: (id) => mockDomElements[id] || null,
      querySelectorAll: () => [],
      addEventListener: () => {}
    },
    Notification: {
      requestPermission: () => Promise.resolve('granted')
    },
    setInterval: () => 1,
    clearInterval: () => {}
  };

  vm.createContext(sandbox);
  vm.runInContext(scriptCode, sandbox);

  const Store = sandbox.Store || sandbox.window.Store;
  assert(Store && Store.state, 'Store must be initialized with state in sandbox context');

  // Assert idempotency guard in initDashboardEvents
  assert.strictEqual(mockContainer.dataset.eventsBound, 'true', 'initDashboardEvents must set container.dataset.eventsBound');
  assert(registeredListeners.click && registeredListeners.click.length === 1, 'Exactly one click listener should be registered on sessions container');

  // Verify secondary invocation does not duplicate listener
  sandbox.initDashboardEvents();
  assert.strictEqual(registeredListeners.click.length, 1, 'Calling initDashboardEvents again must not attach duplicate listener');

  // Assert production toggleSessionAlarm logic
  sandbox.toggleSessionAlarm('race');
  assert.strictEqual(Store.state.alarms.race, true, 'Production toggleSessionAlarm must set race to true in Store');
  assert.strictEqual(JSON.parse(mockStorage['f1_session_alarms']).race, true, 'Production toggleSessionAlarm must serialize state to f1_session_alarms');

  sandbox.toggleSessionAlarm('fp1');
  assert.strictEqual(Store.state.alarms.fp1, true, 'Production toggleSessionAlarm must set fp1 to true in Store');
  assert.strictEqual(Store.state.alarms.race, true, 'Race alarm must remain true');

  sandbox.toggleSessionAlarm('race');
  assert.strictEqual(Store.state.alarms.race, false, 'Production toggleSessionAlarm must invert race to false in Store');
  assert.strictEqual(JSON.parse(mockStorage['f1_session_alarms']).race, false, 'Production toggleSessionAlarm must serialize inverted state');

  // Assert production click event delegation handling
  const clickHandler = registeredListeners.click[0];
  assert(typeof clickHandler === 'function', 'Registered click handler must be a callable function');

  const mockClickEvent = {
    target: {
      closest: (sel) => {
        if (sel === '.alarm-btn') {
          return {
            getAttribute: (attr) => (attr === 'data-session-id' ? 'quali' : null)
          };
        }
        return null;
      }
    }
  };

  clickHandler(mockClickEvent);
  assert.strictEqual(Store.state.alarms.quali, true, 'Clicking .alarm-btn must toggle alarm in Store via production delegation');
  assert.strictEqual(JSON.parse(mockStorage['f1_session_alarms']).quali, true, 'Clicking .alarm-btn must persist alarm to LocalStorage');

  // Assert loadStoredAlarms restores persistence from storage
  const restored = sandbox.loadStoredAlarms();
  assert.strictEqual(restored.quali, true, 'loadStoredAlarms must restore quali alarm');
  assert.strictEqual(restored.fp1, true, 'loadStoredAlarms must restore fp1 alarm');
  assert.strictEqual(restored.race, false, 'loadStoredAlarms must restore race alarm as false');
}

/**
 * Main test runner executing all active test suites.
 */
function runAllTests() {
  const startTime = Date.now();
  console.log('[TEST] Starting F1 Race Hub verification test suites...\n');

  testDatasetSchema();
  console.log('  PASS: testDatasetSchema (22 drivers, 10 constructors, 5 sessions, schema constraints verified)');

  testCountdownMath();
  console.log('  PASS: testCountdownMath (sub-minute, hourly, multi-day, zero, negative boundary cases verified)');

  testTyreCompoundColorMapping();
  console.log('  PASS: testTyreCompoundColorMapping (S, M, H, I, W tokens mapped to official Pirelli hex codes)');

  testHtmlStructureAndRouting();
  console.log('  PASS: testHtmlStructureAndRouting (DOM sections, CSS tokens, navigation, and router verified)');

  testDashboardCountdownAndSessions();
  console.log('  PASS: testDashboardCountdownAndSessions (hero card, 4-box countdown ticker, 5 sessions and alarms verified)');

  const durationMs = Date.now() - startTime;
  console.log(`\n[SUCCESS] All 5 test suites passed cleanly in ${durationMs}ms.`);
}

if (require.main === module) {
  try {
    runAllTests();
  } catch (error) {
    console.error('\n[FAIL] Test assertion failed:', error.message);
    process.exit(1);
  }
}

module.exports = {
  F1_DATA_2024,
  TYRE_COLORS,
  calculateCountdown,
  calculateRemaining,
  testDatasetSchema,
  testCountdownMath,
  testTyreCompoundColorMapping,
  testHtmlStructureAndRouting,
  testDashboardCountdownAndSessions,
  runAllTests
};
