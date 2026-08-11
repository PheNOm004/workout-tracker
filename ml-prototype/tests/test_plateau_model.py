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
