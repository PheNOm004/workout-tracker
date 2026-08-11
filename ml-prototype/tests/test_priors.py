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
