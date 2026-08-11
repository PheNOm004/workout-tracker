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
    assert result > 140.0  # should sit close to the prior, not the single data point


def test_requires_at_least_one_data_point():
    with pytest.raises(ValueError):
        predict_next_one_rep_max([], population_prior=100.0)
