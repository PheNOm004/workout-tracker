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
