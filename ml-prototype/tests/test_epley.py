from src.epley import estimate_one_rep_max


def test_single_rep_returns_weight_unchanged():
    assert estimate_one_rep_max(100.0, 1) == 100.0 * (1 + 1 / 30.0)


def test_multiple_reps_scales_up():
    result = estimate_one_rep_max(80.0, 8)
    assert result == 80.0 * (1 + 8 / 30.0)
    assert result > 80.0


def test_zero_reps_returns_zero_bonus():
    assert estimate_one_rep_max(50.0, 0) == 50.0
