# TimeGo ML Overload/Plateau Prototype Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build and backtest an offline Python prototype (shrinkage regression for overload prediction + slope-based plateau detection) against the current rule-based baseline, to decide whether an on-device Kotlin port is worth scoping as a future sub-project.

**Architecture:** A small, independently-testable Python package (`src/`) with one module per concern (Epley 1RM math, population priors, the shrinkage regression model, the plateau classifier, a Python port of the current rule-based baseline for comparison, and a backtest harness), each covered by pytest using synthetic data. A final Jupyter notebook wires the modules together against real exported data and reports the comparison. No Android/Kotlin code is touched by this plan.

**Tech Stack:** Python 3 (existing Anaconda3/Jupyter environment), pandas, numpy, scikit-learn (`LinearRegression`), scipy (`stats.linregress`), pytest.

## Global Constraints

- Python-only — no Kotlin/Android files are created or modified by this plan (per spec: on-device port is a separate future sub-project).
- Lives entirely under `C:\Users\lsing\AndroidStudioProjects\TimeGo\ml-prototype\` — does not touch the Android app module.
- No network calls at runtime — OpenPowerlifting CSV and the personal `SetLog` export are local files read from `ml-prototype/data/`, both gitignored (large/personal data, not committed).
- Population prior applies only to compound-lift categories mappable to OpenPowerlifting's squat/bench/deadlift columns — everything else uses personal-trend-only prediction (`w = 1`), per spec Section 2.
- Shrinkage weight formula is exactly `w = n / (n + k)` with default `k = 10` (spec Section 2) — keep it a named constant, tune later during backtesting, not hardcoded inline.
- Plateau window is 5 sets, matching the existing Kotlin `PlateauDetection` window (spec Section 3) — named constant, not a magic number.
- REGRESSING classification and the 2.5kg/10%-deload baseline numbers must match the existing Kotlin suggester's constants (spec Section 3) so the backtest baseline is a faithful comparison, not a strawman.
- No in-app export feature — personal data export is a one-time manual SQLite dump (spec Section 1 / Out of scope).

---

## Task 1: Prototype scaffolding

**Files:**
- Create: `ml-prototype/requirements.txt`
- Create: `ml-prototype/pytest.ini`
- Create: `ml-prototype/src/__init__.py`
- Create: `ml-prototype/tests/__init__.py`
- Create: `ml-prototype/.gitignore`
- Create: `ml-prototype/data/README.md`

**Interfaces:**
- Produces: a `src/` package importable from `tests/` via `pytest.ini`'s `rootdir` + `src` on `sys.path` (via `pythonpath` setting), used by every later task's tests.

- [ ] **Step 1: Create the folder structure and dependency list**

`ml-prototype/requirements.txt`:
```
pandas>=2.0
numpy>=1.24
scikit-learn>=1.3
scipy>=1.11
pytest>=7.4
```

`ml-prototype/pytest.ini`:
```ini
[pytest]
pythonpath = .
testpaths = tests
```

`ml-prototype/src/__init__.py`: empty file.

`ml-prototype/tests/__init__.py`: empty file.

`ml-prototype/.gitignore`:
```
data/*.csv
data/*.db
__pycache__/
*.pyc
.ipynb_checkpoints/
```

`ml-prototype/data/README.md`:
```markdown
# Prototype data (not committed)

## OpenPowerlifting CSV
Download the OpenPowerlifting dataset (e.g. via the sergeimakarovv/ML-Powerlifting
Kaggle mirror referenced in the design spec) and place it here as `openpowerlifting.csv`.
Expected columns used by this prototype: Sex, Equipment, BodyweightKg, BestSquatKg,
BestBenchKg, BestDeadliftKg.

## Personal SetLog export
One-time manual export from the TimeGo Room DB (device: SM-S918B):

    adb shell "run-as com.lsing.timego cat /data/data/com.lsing.timego/databases/timego.db" > timego.db
    sqlite3 timego.db ".mode csv" ".headers on" "SELECT exerciseName, timestamp, weightKg, reps FROM SetLog ORDER BY timestamp;" > setlog_export.csv

Place the resulting `setlog_export.csv` in this folder. Not a shipped app feature —
manual dump only, per the design spec's explicit scoping.
```

- [ ] **Step 2: Verify pytest discovers the (currently empty) test suite cleanly**

Run: `cd ml-prototype && pytest --collect-only`
Expected: `collected 0 items` with no errors.

- [ ] **Step 3: Commit**

```bash
cd ml-prototype
git add requirements.txt pytest.ini src/__init__.py tests/__init__.py .gitignore data/README.md
git commit -m "chore(ml-prototype): scaffold Python prototype project"
```

---

## Task 2: Epley 1RM helper

**Files:**
- Create: `ml-prototype/src/epley.py`
- Test: `ml-prototype/tests/test_epley.py`

**Interfaces:**
- Produces: `estimate_one_rep_max(weight_kg: float, reps: int) -> float`, used by Task 3's data loading and every later model.

- [ ] **Step 1: Write the failing test**

`ml-prototype/tests/test_epley.py`:
```python
from src.epley import estimate_one_rep_max


def test_single_rep_returns_weight_unchanged():
    assert estimate_one_rep_max(100.0, 1) == 100.0 * (1 + 1 / 30.0)


def test_multiple_reps_scales_up():
    result = estimate_one_rep_max(80.0, 8)
    assert result == 80.0 * (1 + 8 / 30.0)
    assert result > 80.0


def test_zero_reps_returns_zero_bonus():
    assert estimate_one_rep_max(50.0, 0) == 50.0
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd ml-prototype && pytest tests/test_epley.py -v`
Expected: FAIL with `ModuleNotFoundError: No module named 'src.epley'`

- [ ] **Step 3: Write minimal implementation**

`ml-prototype/src/epley.py`:
```python
def estimate_one_rep_max(weight_kg: float, reps: int) -> float:
    """Epley formula, matching TimeGo's existing Kotlin ProgressMath helper."""
    return weight_kg * (1 + reps / 30.0)
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd ml-prototype && pytest tests/test_epley.py -v`
Expected: 3 passed

- [ ] **Step 5: Commit**

```bash
cd ml-prototype
git add src/epley.py tests/test_epley.py
git commit -m "feat(ml-prototype): add Epley 1RM helper"
```

---

## Task 3: Population priors from OpenPowerlifting data

**Files:**
- Create: `ml-prototype/src/priors.py`
- Test: `ml-prototype/tests/test_priors.py`

**Interfaces:**
- Consumes: nothing from earlier tasks (pure pandas over a DataFrame).
- Produces: `LIFT_CATEGORY_COLUMNS: dict[str, str]`, `build_population_prior(df: pandas.DataFrame, lift_category: str, sex: str) -> float | None`, used by Task 4's overload model.

- [ ] **Step 1: Write the failing test**

`ml-prototype/tests/test_priors.py`:
```python
import pandas as pd
from src.priors import build_population_prior, LIFT_CATEGORY_COLUMNS


def _sample_df():
    return pd.DataFrame({
        "Sex": ["M", "M", "M", "F", "F"],
        "Equipment": ["Raw", "Raw", "Raw", "Raw", "Raw"],
        "BodyweightKg": [80.0, 90.0, 100.0, 60.0, 65.0],
        "BestSquatKg": [140.0, 160.0, 180.0, 90.0, 100.0],
        "BestBenchKg": [100.0, 110.0, 120.0, 50.0, 55.0],
        "BestDeadliftKg": [180.0, 200.0, 220.0, 110.0, 120.0],
    })


def test_known_categories_map_to_columns():
    assert LIFT_CATEGORY_COLUMNS == {
        "squat": "BestSquatKg",
        "bench": "BestBenchKg",
        "deadlift": "BestDeadliftKg",
    }


def test_prior_is_median_bodyweight_ratio_scaled_by_bodyweight():
    df = _sample_df()
    # Male squat/bodyweight ratios: 140/80=1.75, 160/90=1.778, 180/100=1.8 -> median 1.778
    prior = build_population_prior(df, "squat", "M", bodyweight_kg=90.0)
    assert prior is not None
    assert abs(prior - (160.0 / 90.0) * 90.0) < 0.01


def test_unknown_category_returns_none():
    df = _sample_df()
    assert build_population_prior(df, "bicep_curl", "M", bodyweight_kg=80.0) is None


def test_filters_by_sex():
    df = _sample_df()
    male_prior = build_population_prior(df, "bench", "M", bodyweight_kg=100.0)
    female_prior = build_population_prior(df, "bench", "F", bodyweight_kg=100.0)
    assert male_prior != female_prior
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd ml-prototype && pytest tests/test_priors.py -v`
Expected: FAIL with `ModuleNotFoundError: No module named 'src.priors'`

- [ ] **Step 3: Write minimal implementation**

`ml-prototype/src/priors.py`:
```python
import pandas as pd

LIFT_CATEGORY_COLUMNS = {
    "squat": "BestSquatKg",
    "bench": "BestBenchKg",
    "deadlift": "BestDeadliftKg",
}


def build_population_prior(
    df: pd.DataFrame, lift_category: str, sex: str, bodyweight_kg: float
) -> float | None:
    """Median bodyweight-normalized strength ratio for `lift_category`/`sex`,
    scaled to `bodyweight_kg`. Returns None for lift categories with no
    OpenPowerlifting column mapping (isolation/calisthenics work)."""
    column = LIFT_CATEGORY_COLUMNS.get(lift_category)
    if column is None:
        return None

    subset = df[(df["Sex"] == sex) & df[column].notna() & (df["BodyweightKg"] > 0)]
    if subset.empty:
        return None

    ratios = subset[column] / subset["BodyweightKg"]
    return float(ratios.median() * bodyweight_kg)
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd ml-prototype && pytest tests/test_priors.py -v`
Expected: 4 passed

- [ ] **Step 5: Commit**

```bash
cd ml-prototype
git add src/priors.py tests/test_priors.py
git commit -m "feat(ml-prototype): add OpenPowerlifting population prior builder"
```

---

## Task 4: Overload shrinkage regression model

**Files:**
- Create: `ml-prototype/src/overload_model.py`
- Test: `ml-prototype/tests/test_overload_model.py`

**Interfaces:**
- Consumes: nothing directly (takes a `population_prior: float | None` as a plain argument — Task 3's `build_population_prior` is called by the notebook, not by this module, keeping the model testable without pandas).
- Produces: `SHRINKAGE_K = 10`, `shrinkage_weight(n: int, k: int = SHRINKAGE_K) -> float`, `predict_next_one_rep_max(one_rep_maxes: list[float], population_prior: float | None, k: int = SHRINKAGE_K) -> float`, used by Task 7's backtest harness.

- [ ] **Step 1: Write the failing test**

`ml-prototype/tests/test_overload_model.py`:
```python
import pytest
from src.overload_model import shrinkage_weight, predict_next_one_rep_max, SHRINKAGE_K


def test_shrinkage_weight_grows_toward_one_with_more_data():
    assert shrinkage_weight(0) == 0.0
    assert shrinkage_weight(SHRINKAGE_K) == 0.5
    assert shrinkage_weight(1000) > 0.99


def test_no_prior_uses_pure_personal_trend():
    # Perfectly linear uptrend: 100, 102, 104, 106 -> next predicted 108
    one_rms = [100.0, 102.0, 104.0, 106.0]
    result = predict_next_one_rep_max(one_rms, population_prior=None)
    assert abs(result - 108.0) < 0.5


def test_with_prior_blends_toward_prior_when_data_is_sparse():
    one_rms = [100.0]  # n=1, w = 1/(1+10) ~ 0.09, mostly prior
    result = predict_next_one_rep_max(one_rms, population_prior=150.0)
    assert 100.0 < result < 150.0
    assert result < 110.0  # should sit close to the prior, not the single data point


def test_requires_at_least_one_data_point():
    with pytest.raises(ValueError):
        predict_next_one_rep_max([], population_prior=100.0)
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd ml-prototype && pytest tests/test_overload_model.py -v`
Expected: FAIL with `ModuleNotFoundError: No module named 'src.overload_model'`

- [ ] **Step 3: Write minimal implementation**

`ml-prototype/src/overload_model.py`:
```python
import numpy as np
from sklearn.linear_model import LinearRegression

SHRINKAGE_K = 10


def shrinkage_weight(n: int, k: int = SHRINKAGE_K) -> float:
    return n / (n + k)


def _personal_trend_prediction(one_rep_maxes: list[float]) -> float:
    if len(one_rep_maxes) == 1:
        return one_rep_maxes[0]
    x = np.arange(len(one_rep_maxes)).reshape(-1, 1)
    y = np.array(one_rep_maxes)
    model = LinearRegression().fit(x, y)
    next_index = np.array([[len(one_rep_maxes)]])
    return float(model.predict(next_index)[0])


def predict_next_one_rep_max(
    one_rep_maxes: list[float],
    population_prior: float | None,
    k: int = SHRINKAGE_K,
) -> float:
    if not one_rep_maxes:
        raise ValueError("predict_next_one_rep_max requires at least one data point")

    personal = _personal_trend_prediction(one_rep_maxes)
    if population_prior is None:
        return personal

    w = shrinkage_weight(len(one_rep_maxes), k)
    return w * personal + (1 - w) * population_prior
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd ml-prototype && pytest tests/test_overload_model.py -v`
Expected: 4 passed

- [ ] **Step 5: Commit**

```bash
cd ml-prototype
git add src/overload_model.py tests/test_overload_model.py
git commit -m "feat(ml-prototype): add shrinkage regression overload model"
```

---

## Task 5: Slope-based plateau classifier

**Files:**
- Create: `ml-prototype/src/plateau_model.py`
- Test: `ml-prototype/tests/test_plateau_model.py`

**Interfaces:**
- Consumes: nothing from earlier tasks.
- Produces: `PLATEAU_WINDOW = 5`, `PLATEAU_ALPHA = 0.05`, `classify_plateau(values: list[float], window: int = PLATEAU_WINDOW, alpha: float = PLATEAU_ALPHA) -> str` returning one of `"PROGRESSING"`, `"PLATEAUING"`, `"REGRESSING"`, used by Task 7's backtest harness.

- [ ] **Step 1: Write the failing test**

`ml-prototype/tests/test_plateau_model.py`:
```python
from src.plateau_model import classify_plateau


def test_fallback_below_window_regressing_on_two_drops():
    assert classify_plateau([100.0, 95.0]) == "REGRESSING"


def test_fallback_below_window_progressing_otherwise():
    assert classify_plateau([100.0, 102.0]) == "PROGRESSING"


def test_single_value_defaults_progressing():
    assert classify_plateau([100.0]) == "PROGRESSING"


def test_clear_uptrend_over_window_is_progressing():
    assert classify_plateau([100.0, 103.0, 106.0, 109.0, 112.0]) == "PROGRESSING"


def test_two_consecutive_drops_within_window_is_regressing():
    assert classify_plateau([100.0, 103.0, 106.0, 104.0, 101.0]) == "REGRESSING"


def test_flat_oscillation_over_window_is_plateauing():
    assert classify_plateau([100.0, 101.0, 99.0, 100.5, 99.5]) == "PLATEAUING"
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd ml-prototype && pytest tests/test_plateau_model.py -v`
Expected: FAIL with `ModuleNotFoundError: No module named 'src.plateau_model'`

- [ ] **Step 3: Write minimal implementation**

`ml-prototype/src/plateau_model.py`:
```python
from scipy import stats

PLATEAU_WINDOW = 5
PLATEAU_ALPHA = 0.05


def classify_plateau(
    values: list[float], window: int = PLATEAU_WINDOW, alpha: float = PLATEAU_ALPHA
) -> str:
    if len(values) < 2:
        return "PROGRESSING"

    if len(values) < window:
        return "REGRESSING" if values[-1] < values[-2] else "PROGRESSING"

    recent = values[-window:]
    if recent[-1] < recent[-2] and recent[-2] < recent[-3]:
        return "REGRESSING"

    x = list(range(window))
    slope, _intercept, _r, p_value, _stderr = stats.linregress(x, recent)
    if p_value < alpha and slope > 0:
        return "PROGRESSING"
    if p_value < alpha and slope < 0:
        return "REGRESSING"
    return "PLATEAUING"
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd ml-prototype && pytest tests/test_plateau_model.py -v`
Expected: 6 passed

- [ ] **Step 5: Commit**

```bash
cd ml-prototype
git add src/plateau_model.py tests/test_plateau_model.py
git commit -m "feat(ml-prototype): add slope-based plateau classifier"
```

---

## Task 6: Rule-based baseline (Python port, for backtest comparison)

**Files:**
- Create: `ml-prototype/src/baseline.py`
- Test: `ml-prototype/tests/test_baseline.py`

**Interfaces:**
- Consumes: `classify_plateau` from `src/plateau_model.py` (Task 5).
- Produces: `BASELINE_INCREMENT_KG = 2.5`, `BASELINE_DELOAD_PCT = 0.10`, `baseline_predict_next_one_rep_max(one_rep_maxes: list[float]) -> float`, used by Task 7's backtest harness.

- [ ] **Step 1: Write the failing test**

`ml-prototype/tests/test_baseline.py`:
```python
from src.baseline import baseline_predict_next_one_rep_max


def test_regressing_applies_ten_percent_deload():
    # last two values dropping -> REGRESSING
    result = baseline_predict_next_one_rep_max([100.0, 90.0, 80.0])
    assert abs(result - 80.0 * 0.90) < 0.01


def test_progressing_adds_fixed_increment():
    result = baseline_predict_next_one_rep_max([100.0, 102.0])
    assert abs(result - (102.0 + 2.5)) < 0.01


def test_plateauing_holds_flat():
    result = baseline_predict_next_one_rep_max([100.0, 101.0, 99.0, 100.5, 99.5])
    assert abs(result - 99.5) < 0.01
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd ml-prototype && pytest tests/test_baseline.py -v`
Expected: FAIL with `ModuleNotFoundError: No module named 'src.baseline'`

- [ ] **Step 3: Write minimal implementation**

`ml-prototype/src/baseline.py`:
```python
from src.plateau_model import classify_plateau

BASELINE_INCREMENT_KG = 2.5
BASELINE_DELOAD_PCT = 0.10


def baseline_predict_next_one_rep_max(one_rep_maxes: list[float]) -> float:
    """Python port of the current Kotlin RuleBasedOverloadSuggester's decision
    table (spec Section 3), used only as the backtest comparison point."""
    last = one_rep_maxes[-1]
    status = classify_plateau(one_rep_maxes)

    if status == "REGRESSING":
        return last * (1 - BASELINE_DELOAD_PCT)
    if status == "PLATEAUING":
        return last
    return last + BASELINE_INCREMENT_KG
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd ml-prototype && pytest tests/test_baseline.py -v`
Expected: 3 passed

- [ ] **Step 5: Commit**

```bash
cd ml-prototype
git add src/baseline.py tests/test_baseline.py
git commit -m "feat(ml-prototype): add rule-based baseline for backtest comparison"
```

---

## Task 7: Backtest harness

**Files:**
- Create: `ml-prototype/src/backtest.py`
- Test: `ml-prototype/tests/test_backtest.py`

**Interfaces:**
- Consumes: `predict_next_one_rep_max` (Task 4), `baseline_predict_next_one_rep_max` (Task 6).
- Produces: `MIN_BACKTEST_HISTORY = 3`, `backtest_exercise(one_rep_maxes: list[float], population_prior: float | None, min_history: int = MIN_BACKTEST_HISTORY) -> list[dict]`, `summarize_backtest(results: list[dict]) -> dict`, used by Task 8's notebook.

- [ ] **Step 1: Write the failing test**

`ml-prototype/tests/test_backtest.py`:
```python
from src.backtest import backtest_exercise, summarize_backtest


def test_backtest_produces_one_result_per_held_out_point():
    # 6 points, min_history=3 -> held-out points are indices 3,4,5 -> 3 results
    one_rms = [100.0, 102.0, 104.0, 106.0, 108.0, 110.0]
    results = backtest_exercise(one_rms, population_prior=None)
    assert len(results) == 3
    for r in results:
        assert set(r.keys()) == {"index", "actual", "ml_pred", "baseline_pred", "ml_error", "baseline_error"}
        assert r["ml_error"] >= 0
        assert r["baseline_error"] >= 0


def test_summarize_backtest_computes_mean_absolute_error():
    results = [
        {"index": 3, "actual": 100.0, "ml_pred": 101.0, "baseline_pred": 105.0, "ml_error": 1.0, "baseline_error": 5.0},
        {"index": 4, "actual": 110.0, "ml_pred": 109.0, "baseline_pred": 100.0, "ml_error": 1.0, "baseline_error": 10.0},
    ]
    summary = summarize_backtest(results)
    assert summary == {"ml_mae": 1.0, "baseline_mae": 7.5, "n": 2}


def test_too_short_history_returns_no_results():
    assert backtest_exercise([100.0, 102.0], population_prior=None) == []
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd ml-prototype && pytest tests/test_backtest.py -v`
Expected: FAIL with `ModuleNotFoundError: No module named 'src.backtest'`

- [ ] **Step 3: Write minimal implementation**

`ml-prototype/src/backtest.py`:
```python
import numpy as np

from src.overload_model import predict_next_one_rep_max
from src.baseline import baseline_predict_next_one_rep_max

MIN_BACKTEST_HISTORY = 3


def backtest_exercise(
    one_rep_maxes: list[float],
    population_prior: float | None,
    min_history: int = MIN_BACKTEST_HISTORY,
) -> list[dict]:
    results = []
    for i in range(min_history, len(one_rep_maxes)):
        history = one_rep_maxes[:i]
        actual = one_rep_maxes[i]

        ml_pred = predict_next_one_rep_max(history, population_prior)
        baseline_pred = baseline_predict_next_one_rep_max(history)

        results.append({
            "index": i,
            "actual": actual,
            "ml_pred": ml_pred,
            "baseline_pred": baseline_pred,
            "ml_error": abs(ml_pred - actual),
            "baseline_error": abs(baseline_pred - actual),
        })
    return results


def summarize_backtest(results: list[dict]) -> dict:
    if not results:
        return {"ml_mae": None, "baseline_mae": None, "n": 0}

    ml_errors = [r["ml_error"] for r in results]
    baseline_errors = [r["baseline_error"] for r in results]
    return {
        "ml_mae": float(np.mean(ml_errors)),
        "baseline_mae": float(np.mean(baseline_errors)),
        "n": len(results),
    }
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd ml-prototype && pytest tests/test_backtest.py -v`
Expected: 3 passed

- [ ] **Step 5: Run the full test suite to confirm nothing else broke**

Run: `cd ml-prototype && pytest -v`
Expected: all tests across every module (Tasks 2–7) pass.

- [ ] **Step 6: Commit**

```bash
cd ml-prototype
git add src/backtest.py tests/test_backtest.py
git commit -m "feat(ml-prototype): add backtest harness comparing ML model vs baseline"
```

---

## Task 8: Notebook — run against real data and report results

**Files:**
- Create: `ml-prototype/overload_plateau_prototype.ipynb`

**Interfaces:**
- Consumes: `build_population_prior`/`LIFT_CATEGORY_COLUMNS` (Task 3), `backtest_exercise`/`summarize_backtest` (Task 7), `estimate_one_rep_max` (Task 2). Also reads `data/openpowerlifting.csv` and `data/setlog_export.csv` (manual exports per Task 1's `data/README.md`).
- Produces: the final verdict artifact for this design — no further tasks consume this notebook's output programmatically.

**This task requires the user present**: it needs the real OpenPowerlifting CSV downloaded and the real `SetLog` export dumped from the phone (Task 1's `data/README.md` steps) before it can run against real data — neither is available to an automated worker.

- [ ] **Step 1: Confirm both data files are in place**

Check: `ml-prototype/data/openpowerlifting.csv` and `ml-prototype/data/setlog_export.csv` both exist (per `data/README.md`). If not, stop here and get them first — this task cannot proceed on synthetic data alone, since its whole purpose is a real-data verdict.

- [ ] **Step 2: Write the notebook's data-loading cell**

```python
import sys
sys.path.insert(0, ".")

import pandas as pd
from src.epley import estimate_one_rep_max
from src.priors import build_population_prior, LIFT_CATEGORY_COLUMNS
from src.backtest import backtest_exercise, summarize_backtest

opl_df = pd.read_csv("data/openpowerlifting.csv")
setlog_df = pd.read_csv("data/setlog_export.csv")
setlog_df["one_rep_max"] = setlog_df.apply(
    lambda row: estimate_one_rep_max(row["weightKg"], row["reps"]), axis=1
)
setlog_df = setlog_df.sort_values(["exerciseName", "timestamp"])
```

- [ ] **Step 3: Write the per-exercise backtest cell**

```python
YOUR_SEX = "M"  # adjust to match the OpenPowerlifting Sex column convention
LIFT_CATEGORY_BY_EXERCISE = {
    # map your own exercise names to a LIFT_CATEGORY_COLUMNS key where applicable,
    # e.g. "Barbell Back Squat": "squat", "Conventional Deadlift": "deadlift"
}

all_results = {}
for exercise_name, group in setlog_df.groupby("exerciseName"):
    one_rms = group["one_rep_max"].tolist()
    lift_category = LIFT_CATEGORY_BY_EXERCISE.get(exercise_name)
    prior = None
    if lift_category is not None and len(one_rms) > 0:
        bodyweight = 80.0  # replace with your latest logged BodyMetric weight
        prior = build_population_prior(opl_df, lift_category, YOUR_SEX, bodyweight)

    results = backtest_exercise(one_rms, population_prior=prior)
    if results:
        all_results[exercise_name] = summarize_backtest(results)

report = pd.DataFrame(all_results).T
report["ml_wins"] = report["ml_mae"] < report["baseline_mae"]
report
```

- [ ] **Step 4: Write the summary verdict cell**

```python
overall_ml_mae = report["ml_mae"].mean()
overall_baseline_mae = report["baseline_mae"].mean()
win_rate = report["ml_wins"].mean()

print(f"ML model overall MAE: {overall_ml_mae:.2f}")
print(f"Baseline overall MAE: {overall_baseline_mae:.2f}")
print(f"ML model beats baseline on {win_rate:.0%} of exercises")
print()
print("Verdict: proceed to Kotlin port" if overall_ml_mae < overall_baseline_mae
      else "Verdict: baseline still wins, do not port yet")
```

- [ ] **Step 5: Run the whole notebook top to bottom**

In Jupyter: Kernel → Restart & Run All. Confirm no cell errors and the verdict cell prints a clear conclusion.

- [ ] **Step 6: Commit**

```bash
cd ml-prototype
git add overload_plateau_prototype.ipynb
git commit -m "feat(ml-prototype): run backtest notebook against real data, report verdict"
```

Note: `data/openpowerlifting.csv` and `data/setlog_export.csv` themselves stay gitignored (Task 1) — only the notebook is committed.

---

## Self-Review Notes

- **Spec coverage**: Section 1 (data pipeline) → Task 1 + Task 8 Step 1-2. Section 2 (overload model) → Task 3 + Task 4. Section 3 (plateau model) → Task 5. Section 4 (evaluation) → Task 6 + Task 7. Section 5 (deliverable) → Task 8. Out-of-scope items (no Kotlin, no in-app export) are respected — no task touches the Android module.
- **Type consistency checked**: `predict_next_one_rep_max` (Task 4) and `baseline_predict_next_one_rep_max` (Task 6) both consumed by `backtest_exercise` (Task 7) with matching `list[float]` signatures; `classify_plateau` (Task 5) consumed by `baseline.py` (Task 6) with the same signature throughout.
- **No placeholders**: every step has real, runnable code and concrete expected test output.
