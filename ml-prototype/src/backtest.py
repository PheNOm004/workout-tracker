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
