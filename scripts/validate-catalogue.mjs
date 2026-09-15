import fs from "node:fs";
import path from "node:path";
import process from "node:process";

const root = path.resolve(import.meta.dirname, "..");
const sourcePath = path.join(root, "app/src/main/java/com/lsing/timego/data/SeedExercises.kt");
const cataloguePath = path.join(root, "catalogue/exercises.json");
const allowed = {
  difficulty: new Set(["BEGINNER", "INTERMEDIATE", "ADVANCED"]),
  complexity: new Set(["LOW", "MEDIUM", "HIGH"]),
  reviewStatus: new Set(["METADATA_ONLY", "REVIEWED_COMPLETE", "DEPRECATED"]),
};

function slug(name) {
  return `timego.seed.v1.${name.toLowerCase().replace(/[^a-z0-9]+/g, "-").replace(/^-|-$/g, "")}`;
}

function parseSeeds() {
  const text = fs.readFileSync(sourcePath, "utf8");
  const call = /^\s*(strength|calisthenics|warmup|cardio)\("([^"]+)"(.*)\),?\s*$/;
  const entries = [];
  for (const line of text.split(/\r?\n/)) {
    const match = line.match(call);
    if (!match) continue;
    const [, kind, name, rest] = match;
    const muscles = [...rest.matchAll(/MuscleGroup\.([A-Z_]+)/g)].map((m) => m[1]);
    const weightSection = rest.match(/weights\s*=\s*mapOf\((.*)\)/)?.[1] ?? "";
    const muscleWeights = Object.fromEntries(
      [...weightSection.matchAll(/MuscleGroup\.([A-Z_]+)\s+to\s+(\d+)/g)].map((m) => [m[1], Number(m[2])]),
    );
    const muscleGroups = [...new Set(muscles.filter((m) => !(m in muscleWeights) || muscles.indexOf(m) < rest.indexOf("weights")))];
    const loggingOverride = rest.match(/loggingType\s*=\s*LoggingType\.([A-Z_]+)/)?.[1];
    entries.push({
      key: slug(name),
      name,
      aliases: [],
      category: kind === "strength" ? "STRENGTH" : kind === "calisthenics" ? "CALISTHENICS" : kind.toUpperCase(),
      loggingType: loggingOverride ?? (kind === "strength" || kind === "calisthenics" ? "WEIGHT_REPS" : "DURATION_DISTANCE"),
      muscleGroups,
      muscleWeights,
      equipment: [],
      difficulty: "INTERMEDIATE",
      complexity: "MEDIUM",
      purpose: "",
      setup: [],
      steps: [],
      cues: [],
      mistakes: [],
      easierVariationKey: null,
      harderVariationKeys: [],
      reviewStatus: "METADATA_ONLY",
    });
  }
  return entries;
}

function generate() {
  const existing = fs.existsSync(cataloguePath)
    ? new Map(JSON.parse(fs.readFileSync(cataloguePath, "utf8")).exercises.map((e) => [e.key, e]))
    : new Map();
  const exercises = parseSeeds().map((seed) => ({ ...seed, ...(existing.get(seed.key) ?? {}), key: seed.key, name: seed.name }));
  fs.mkdirSync(path.dirname(cataloguePath), { recursive: true });
  fs.writeFileSync(cataloguePath, `${JSON.stringify({ schemaVersion: 1, catalogueVersion: 1, exercises }, null, 2)}\n`);
  console.log(`Generated ${exercises.length} catalogue entries.`);
}

function validate({ allowIncomplete }) {
  const document = JSON.parse(fs.readFileSync(cataloguePath, "utf8"));
  const errors = [];
  if (document.schemaVersion !== 1) errors.push("schemaVersion must be 1");
  if (!Number.isInteger(document.catalogueVersion) || document.catalogueVersion < 1) errors.push("catalogueVersion must be a positive integer");
  if (!Array.isArray(document.exercises) || document.exercises.length !== 820) errors.push(`expected 820 exercises, found ${document.exercises?.length ?? 0}`);
  const keys = new Set();
  for (const [index, entry] of (document.exercises ?? []).entries()) {
    const at = `exercises[${index}]`;
    if (!entry.key || keys.has(entry.key)) errors.push(`${at}: missing or duplicate key ${entry.key}`);
    keys.add(entry.key);
    for (const field of ["name", "category", "loggingType", "purpose"]) if (typeof entry[field] !== "string") errors.push(`${at}.${field} must be a string`);
    for (const field of ["aliases", "muscleGroups", "equipment", "setup", "steps", "cues", "mistakes", "harderVariationKeys"]) if (!Array.isArray(entry[field])) errors.push(`${at}.${field} must be an array`);
    for (const field of Object.keys(allowed)) if (!allowed[field].has(entry[field])) errors.push(`${at}.${field} is unsupported: ${entry[field]}`);
    for (const [muscle, weight] of Object.entries(entry.muscleWeights ?? {})) if (!Number.isInteger(weight) || weight < 0 || weight > 100) errors.push(`${at}.muscleWeights.${muscle} must be an integer from 0 to 100`);
    if (entry.reviewStatus === "REVIEWED_COMPLETE") {
      for (const field of ["purpose", "setup", "steps", "cues", "mistakes"]) if (entry[field]?.length === 0) errors.push(`${at}.${field} cannot be empty when reviewed`);
    }
  }
  for (const entry of document.exercises ?? []) {
    const links = [entry.easierVariationKey, ...(entry.harderVariationKeys ?? [])].filter(Boolean);
    for (const target of links) {
      if (target === entry.key) errors.push(`${entry.key}: variation cannot point to itself`);
      if (!keys.has(target)) errors.push(`${entry.key}: missing variation target ${target}`);
    }
  }
  for (const start of document.exercises ?? []) {
    const visited = new Set([start.key]);
    let cursor = start;
    while (cursor?.easierVariationKey) {
      if (visited.has(cursor.easierVariationKey)) { errors.push(`${start.key}: cycle in easier variations`); break; }
      visited.add(cursor.easierVariationKey);
      cursor = document.exercises.find((e) => e.key === cursor.easierVariationKey);
    }
  }
  const complete = (document.exercises ?? []).filter((e) => e.reviewStatus === "REVIEWED_COMPLETE").length;
  if (!allowIncomplete && complete !== 200) errors.push(`expected exactly 200 REVIEWED_COMPLETE entries, found ${complete}`);
  if (errors.length) throw new Error(`Catalogue validation failed:\n- ${errors.join("\n- ")}`);
  console.log(`Catalogue valid: ${document.exercises.length} entries, ${complete} reviewed complete.`);
}

if (process.argv.includes("--generate")) generate();
validate({ allowIncomplete: process.argv.includes("--allow-incomplete") });
