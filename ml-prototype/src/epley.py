def estimate_one_rep_max(weight_kg: float, reps: int) -> float:
    """Epley formula, matching TimeGo's existing Kotlin ProgressMath helper."""
    return weight_kg * (1 + reps / 30.0)
