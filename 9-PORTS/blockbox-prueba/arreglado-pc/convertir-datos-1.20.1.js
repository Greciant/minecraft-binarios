// Pasa los datos de Block Box (formato 1.21 de NeoForge) al formato de Forge 1.20.1.
// Uso: node convertir-datos-1.20.1.js   (desde la carpeta del proyecto)
const fs = require('fs');
const path = require('path');

const DATA = path.join(__dirname, 'src', 'main', 'resources', 'data');

// 1) Carpetas: 1.21 usa singular, 1.20.1 plural
const RENAMES = [
  ['tags/block', 'tags/blocks'],
  ['tags/item', 'tags/items'],
  ['recipe', 'recipes'],
  ['loot_table', 'loot_tables'],
  ['advancement', 'advancements'],
];
for (const ns of fs.readdirSync(DATA)) {
  for (const [from, to] of RENAMES) {
    const a = path.join(DATA, ns, from), b = path.join(DATA, ns, to);
    if (fs.existsSync(a) && !fs.existsSync(b)) fs.renameSync(a, b);
  }
}

function walk(dir, out = []) {
  if (!fs.existsSync(dir)) return out;
  for (const f of fs.readdirSync(dir)) {
    const p = path.join(dir, f);
    if (fs.statSync(p).isDirectory()) walk(p, out); else if (p.endsWith('.json')) out.push(p);
  }
  return out;
}

// En 1.20.1 los tags comunes de Forge son forge:, no c:
const fixTag = t => t.replace(/^c:(dyes|ingots)\//, 'forge:$1/');
function fixIngredients(o) {
  if (Array.isArray(o)) return o.forEach(fixIngredients);
  if (o && typeof o === 'object') {
    if (typeof o.tag === 'string') o.tag = fixTag(o.tag);
    for (const k of Object.keys(o)) fixIngredients(o[k]);
  }
}

let nRec = 0, nAdv = 0;
for (const ns of fs.readdirSync(DATA)) {
  // 2) Recetas: resultado {id,count} -> formato 1.20.1
  for (const f of walk(path.join(DATA, ns, 'recipes'))) {
    const r = JSON.parse(fs.readFileSync(f, 'utf8'));
    fixIngredients(r.key); fixIngredients(r.ingredients); fixIngredients(r.ingredient);
    const res = r.result;
    if (res && typeof res === 'object' && res.id) {
      if (r.type === 'minecraft:stonecutting') {
        r.result = res.id; r.count = res.count || 1;
      } else if (/smelting|blasting|smoking|campfire_cooking/.test(r.type)) {
        r.result = res.id;
      } else {
        r.result = { item: res.id };
        if (res.count && res.count !== 1) r.result.count = res.count;
      }
    }
    fs.writeFileSync(f, JSON.stringify(r, null, 2) + '\n');
    nRec++;
  }
  // 3) Logros: predicado de item "items": "id" -> ["id"]
  for (const f of walk(path.join(DATA, ns, 'advancements'))) {
    const a = JSON.parse(fs.readFileSync(f, 'utf8'));
    for (const c of Object.values(a.criteria || {})) {
      const items = c.conditions && c.conditions.items;
      if (Array.isArray(items)) for (const p of items) {
        if (typeof p.items === 'string') {
          if (p.items.startsWith('#')) { p.tag = fixTag(p.items.slice(1)); delete p.items; }
          else p.items = [p.items];
        }
      }
    }
    fs.writeFileSync(f, JSON.stringify(a, null, 2) + '\n');
    nAdv++;
  }
}
console.log(`recetas: ${nRec}, logros: ${nAdv}`);
