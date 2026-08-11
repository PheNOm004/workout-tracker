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
