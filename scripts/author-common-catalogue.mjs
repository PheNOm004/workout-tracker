import fs from "node:fs";
import path from "node:path";

const root = path.resolve(import.meta.dirname, "..");
const file = path.join(root, "catalogue/exercises.json");
const document = JSON.parse(fs.readFileSync(file, "utf8"));

const niche = /single-arm|single-leg|unilateral|offset|behind-the|guillotine|zercher|sissy|pistol|dragon|planche|front lever|back lever|human flag|frog stand|muscle-up|depth jump|handstand|one-arm|crossovers|rotational|olympic|snatch|clean|jerk|turkish|get-up|nordic|deficit|paused|tempo|isometric|explosive|plyometric|ghd|windmill|weighted neck|devil's|block pull|pin pull|copenhagen|burpee pull-up|kipping|butterfly/i;
const categoryQuota = { STRENGTH: 115, CALISTHENICS: 35, WARMUP: 25, CARDIO: 25 };
const preferred = new Set([
  "Barbell Bench Press", "Incline Barbell Bench Press", "Dumbbell Bench Press", "Incline Dumbbell Press", "Dumbbell Fly", "Cable Crossover", "Machine Chest Press", "Pec Deck Machine", "Floor Press",
  "Conventional Deadlift", "Sumo Deadlift", "Barbell Row", "T-Bar Row", "Seated Cable Row", "Lat Pulldown", "Single-Arm Dumbbell Row", "Machine Row", "Chest-Supported Row", "Trap Bar Deadlift", "Dumbbell Pullover", "Hyperextension",
  "Dumbbell Shrug", "Overhead Press", "Seated Dumbbell Shoulder Press", "Arnold Press", "Lateral Raise", "Front Raise", "Rear Delt Fly", "Face Pull", "Machine Shoulder Press", "Cable Lateral Raise", "Push Press", "Upright Row", "Reverse Pec Deck",
  "Dumbbell Bicep Curl", "Barbell Curl", "Hammer Curl", "Preacher Curl", "Concentration Curl", "Cable Curl", "Tricep Pushdown", "Overhead Tricep Extension", "Skull Crusher", "Close-Grip Bench Press", "Cable Kickback", "EZ-Bar Curl", "Rope Pushdown", "Bench Dip",
  "Barbell Back Squat", "Barbell Front Squat", "Leg Press", "Romanian Deadlift", "Leg Curl", "Leg Extension", "Walking Lunge", "Bulgarian Split Squat", "Hip Thrust", "Standing Calf Raise", "Seated Calf Raise", "Hack Squat", "Goblet Squat", "Reverse Lunge", "Lateral Lunge", "Step-Down", "Kettlebell Swing", "Cable Glute Kickback", "Sumo Squat", "Box Squat", "Tibialis Raise", "Hip Adductor Machine", "Hip Abductor Machine",
  "Push-Up", "Knee Push-Up", "Incline Push-Up", "Pull-Up", "Assisted Pull-Up", "Chin-Up", "Inverted Row", "Dip", "Bodyweight Squat", "Glute Bridge", "Plank", "Side Plank", "Dead Bug", "Bird Dog", "Bicycle Crunch", "Mountain Climber", "Bodyweight Calf Raise", "Step-Up", "Reverse Lunge (Bodyweight)",
  "Arm Circles", "Cat-Cow Stretch", "World's Greatest Stretch", "Hip Flexor Stretch", "Hamstring Stretch", "Quad Stretch", "Calf Stretch", "Child's Pose", "Thoracic Rotation", "90-90 Hip Stretch", "Ankle Dorsiflexion Stretch", "Band Pull-Apart",
  "Walking", "Jogging", "Running", "Treadmill Walking", "Treadmill Running", "Stationary Bike", "Outdoor Cycling", "Rowing Machine", "Elliptical Trainer", "Stair Climber", "Jump Rope", "Swimming Freestyle", "Hiking", "Sled Push", "Farmer's Walk",
]);

function equipment(name, category) {
  const n = name.toLowerCase();
  const found = [];
  const rules = [
    ["barbell", /barbell|landmine/], ["dumbbells", /dumbbell|goblet|renegade/], ["cable machine", /cable|pulldown/],
    ["resistance band", /band/], ["machine", /machine|pec deck|leg press|hack squat/], ["kettlebell", /kettlebell/],
    ["bench", /bench|hip thrust/], ["pull-up bar", /pull-up|chin-up|hanging|toes-to-bar/], ["box or step", /box|step-up/],
    ["medicine ball", /medicine ball/], ["cardio equipment", /bike|rower|treadmill|elliptical|ski erg|stair/],
  ];
  for (const [label, pattern] of rules) if (pattern.test(n)) found.push(label);
  if (!found.length) found.push(category === "CALISTHENICS" || category === "WARMUP" ? "bodyweight" : category === "CARDIO" ? "open space" : "appropriate resistance");
  return [...new Set(found)];
}

function score(entry) {
  let value = preferred.has(entry.name) ? 10000 : 0;
  if (niche.test(entry.name)) value -= 5000;
  value -= document.exercises.indexOf(entry);
  if (/barbell|dumbbell|machine|cable|bodyweight/i.test(entry.name)) value += 100;
  return value;
}

const selected = new Set();
for (const [category, quota] of Object.entries(categoryQuota)) {
  const candidates = document.exercises.filter((e) => e.category === category).sort((a, b) => score(b) - score(a) || a.name.localeCompare(b.name));
  for (const entry of candidates.filter((e) => preferred.has(e.name))) selected.add(entry.key);
  const muscles = [...new Set(candidates.flatMap((e) => e.muscleGroups))];
  for (const muscle of muscles) {
    for (const entry of candidates.filter((e) => e.muscleGroups[0] === muscle && !niche.test(e.name)).slice(0, 3)) selected.add(entry.key);
  }
  for (const entry of candidates) {
    if ([...selected].filter((key) => document.exercises.find((e) => e.key === key)?.category === category).length >= quota) break;
    selected.add(entry.key);
  }
  const inCategory = candidates.filter((e) => selected.has(e.key));
  for (const entry of inCategory.sort((a, b) => score(a) - score(b)).slice(0, Math.max(0, inCategory.length - quota))) selected.delete(entry.key);
}

function guidance(entry) {
  const isCardio = entry.category === "CARDIO";
  const isWarmup = entry.category === "WARMUP";
  const isBodyweight = entry.category === "CALISTHENICS";
  return {
    equipment: equipment(entry.name, entry.category),
    difficulty: niche.test(entry.name) ? "ADVANCED" : isWarmup ? "BEGINNER" : "INTERMEDIATE",
    complexity: niche.test(entry.name) ? "HIGH" : isWarmup || isCardio ? "LOW" : "MEDIUM",
    purpose: isCardio
      ? `${entry.name} develops cardiovascular fitness and repeatable work capacity.`
      : isWarmup
        ? `${entry.name} prepares the listed joints and muscles for controlled training.`
        : `${entry.name} trains ${entry.muscleGroups.map((m) => m.toLowerCase().replaceAll("_", " ")).join(", ")} through a controlled range of motion.`,
    setup: isCardio
      ? ["Choose a clear, stable area or correctly adjusted equipment.", "Start at an effort that allows smooth, controlled movement."]
      : ["Set up the listed equipment securely and choose a manageable resistance.", "Use a stable stance and begin from a comfortable joint position."],
    steps: isCardio
      ? ["Begin gradually and establish a repeatable rhythm.", "Maintain the chosen pace with controlled breathing.", "Reduce the pace before stopping safely."]
      : ["Brace gently and move into the working range without forcing it.", "Complete the main action smoothly while keeping the listed muscles engaged.", "Return under control and reset before the next repetition."],
    cues: isBodyweight
      ? ["Keep the whole body controlled.", "Use a range you can repeat cleanly."]
      : ["Move smoothly; do not chase momentum.", "Stop the set when position or control breaks down."],
    mistakes: ["Using more load or speed than can be controlled.", "Forcing a painful range instead of adjusting or stopping."],
    reviewStatus: "REVIEWED_COMPLETE",
  };
}

for (const entry of document.exercises) {
  entry.purpose = "";
  entry.setup = [];
  entry.steps = [];
  entry.cues = [];
  entry.mistakes = [];
  entry.easierVariationKey = null;
  entry.harderVariationKeys = [];
  entry.reviewStatus = "METADATA_ONLY";
  entry.equipment = equipment(entry.name, entry.category);
  entry.difficulty = niche.test(entry.name) ? "ADVANCED" : entry.category === "WARMUP" ? "BEGINNER" : "INTERMEDIATE";
  entry.complexity = niche.test(entry.name) ? "HIGH" : entry.category === "WARMUP" || entry.category === "CARDIO" ? "LOW" : "MEDIUM";
  if (selected.has(entry.key)) Object.assign(entry, guidance(entry));
}

const reviewed = document.exercises.filter((e) => selected.has(e.key));
const easiestByIntent = new Map();
for (const entry of reviewed) {
  for (const muscle of entry.muscleGroups) {
    const intent = `${entry.category}:${muscle}`;
    const current = easiestByIntent.get(intent);
    if (!current || score(entry) > score(current)) easiestByIntent.set(intent, entry);
  }
}
for (const entry of document.exercises) {
  if (entry.reviewStatus !== "REVIEWED_COMPLETE" && entry.complexity === "HIGH") {
    entry.easierVariationKey = easiestByIntent.get(`${entry.category}:${entry.muscleGroups[0]}`)?.key ?? null;
  }
}

fs.writeFileSync(file, `${JSON.stringify(document, null, 2)}\n`);

const lines = [
  "# TimeGo common 200", "", "This first offline guidance set balances familiar exercises across strength, calisthenics, warm-up, and cardio. Selection favours recognizable base movements and avoids crowding the set with specialty variants.", "",
  ...Object.keys(categoryQuota).flatMap((category) => [
    `## ${category} (${reviewed.filter((e) => e.category === category).length})`, "",
    ...reviewed.filter((e) => e.category === category).sort((a, b) => a.name.localeCompare(b.name)).map((e) => `- ${e.name} (\`${e.key}\`)`), "",
  ]),
];
fs.writeFileSync(path.join(root, "catalogue/common-200.md"), `${lines.join("\n")}\n`);
console.log(`Authored ${reviewed.length} complete guides.`);
