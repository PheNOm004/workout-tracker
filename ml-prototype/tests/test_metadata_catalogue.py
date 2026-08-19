import pytest
import json
from pathlib import Path

from src.metadata_catalogue import load_catalogue, unanchored_coordinates


def catalogue_payload(entries):
    return {
        "catalogue_version": "2026-08-19.v1",
        "coordinate_schema": ["vertical_pull", "horizontal_pull", "trunk"],
        "entries": entries,
    }


def anchor_entry(key="timego.seed.v1.pull-up"):
    return {
        "catalogue_key": key,
        "logging_type": "WEIGHT_REPS",
        "demand_coordinates": {"vertical_pull": 1.0, "trunk": 0.2},
        "equipment": ["bar"],
        "bodyweight_supported": True,
        "static_exclusions": ["no_overhead_bar"],
    }


def test_catalogue_has_a_stable_hash_independent_of_entry_order():
    first = load_catalogue(catalogue_payload([anchor_entry(), anchor_entry("timego.seed.v1.row")]))
    reversed_order = load_catalogue(catalogue_payload([anchor_entry("timego.seed.v1.row"), anchor_entry()]))

    assert first.metadata_hash == reversed_order.metadata_hash
    assert first.catalogue_version == "2026-08-19.v1"


def test_catalogue_rejects_a_hidden_ladder_or_rank_field():
    entry = anchor_entry()
    entry["next_exercise"] = "timego.seed.v1.advanced-pull-up"

    with pytest.raises(ValueError, match="forbidden progression field"):
        load_catalogue(catalogue_payload([entry]))


def test_catalogue_rejects_coordinates_outside_the_declared_fixed_schema():
    entry = anchor_entry()
    entry["demand_coordinates"] = {"lever_strength": 1.0}

    with pytest.raises(ValueError, match="unknown coordinate"):
        load_catalogue(catalogue_payload([entry]))


def test_catalogue_requires_non_room_immutable_keys():
    entry = anchor_entry(key="42")

    with pytest.raises(ValueError, match="immutable catalogue key"):
        load_catalogue(catalogue_payload([entry]))


def test_reviewed_catalogue_draft_has_an_anchor_pair_for_every_coordinate():
    path = Path(__file__).parents[1] / "metadata" / "adaptive-coach-catalogue.2026-08-19.v1.json"
    catalogue = load_catalogue(json.loads(path.read_text(encoding="utf-8")))

    assert unanchored_coordinates(catalogue) == set()


def test_reviewed_catalogue_resolves_seed_aliases_but_excludes_unknown_custom_names():
    path = Path(__file__).parents[1] / "metadata" / "adaptive-coach-catalogue.2026-08-19.v1.json"
    catalogue = load_catalogue(json.loads(path.read_text(encoding="utf-8")))

    assert catalogue.entry_for_seed_alias("Pull-Up").catalogue_key == "timego.seed.v1.pull-up"
    assert catalogue.entry_for_seed_alias("A personal custom exercise") is None
