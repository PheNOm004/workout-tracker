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
