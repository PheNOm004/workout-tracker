"""Versioned, declarative exercise metadata for adaptive-coach research.

The catalogue intentionally cannot represent a rank, prerequisite, or next exercise. It identifies
what an exercise measures; it does not prescribe an exercise sequence.
"""

from __future__ import annotations

from dataclasses import dataclass
from hashlib import sha256
import json
import re
from typing import Any, Mapping


_LOGGING_TYPES = {"WEIGHT_REPS", "HOLD", "DURATION_DISTANCE"}
_ENTRY_FIELDS = {
    "catalogue_key",
    "logging_type",
    "demand_coordinates",
    "equipment",
    "bodyweight_supported",
    "static_exclusions",
}
_FORBIDDEN_FIELD_TOKENS = {
    "next",
    "next_exercise",
    "nextexercise",
    "rank",
    "progression",
    "prerequisite",
    "prerequisites",
    "ladder",
}
_KEY_PATTERN = re.compile(r"^timego\.[a-z][a-z0-9._-]*$")


@dataclass(frozen=True)
class MetadataEntry:
    catalogue_key: str
    logging_type: str
    demand_coordinates: tuple[tuple[str, float], ...]
    equipment: tuple[str, ...]
    bodyweight_supported: bool
    static_exclusions: tuple[str, ...]


@dataclass(frozen=True)
class MetadataCatalogue:
    catalogue_version: str
    coordinate_schema: tuple[str, ...]
    entries: tuple[MetadataEntry, ...]
    seed_aliases: tuple[tuple[str, str], ...]
    metadata_hash: str

    def entry_for_seed_alias(self, name: str) -> MetadataEntry | None:
        key = dict(self.seed_aliases).get(name)
        return next((entry for entry in self.entries if entry.catalogue_key == key), None)


def _canonical_json(value: Any) -> str:
    return json.dumps(value, sort_keys=True, separators=(",", ":"), ensure_ascii=True)


def _entry_from_payload(payload: Mapping[str, Any], schema: set[str]) -> MetadataEntry:
    actual_fields = set(payload)
    forbidden = actual_fields & _FORBIDDEN_FIELD_TOKENS
    if forbidden:
        raise ValueError(f"forbidden progression field: {sorted(forbidden)[0]}")
    unexpected = actual_fields - _ENTRY_FIELDS
    if unexpected:
        raise ValueError(f"unsupported metadata field: {sorted(unexpected)[0]}")

    key = payload.get("catalogue_key")
    if not isinstance(key, str) or not _KEY_PATTERN.fullmatch(key):
        raise ValueError("immutable catalogue key must be a namespaced, non-Room identifier")

    logging_type = payload.get("logging_type")
    if logging_type not in _LOGGING_TYPES:
        raise ValueError("unsupported logging type")

    raw_demands = payload.get("demand_coordinates")
    if not isinstance(raw_demands, Mapping) or not raw_demands:
        raise ValueError("demand_coordinates must be a non-empty mapping")
    demands: list[tuple[str, float]] = []
    for coordinate, weight in raw_demands.items():
        if coordinate not in schema:
            raise ValueError(f"unknown coordinate: {coordinate}")
        if not isinstance(weight, (int, float)) or isinstance(weight, bool) or not 0.0 < float(weight) <= 1.0:
            raise ValueError("demand coordinate weight must be in (0, 1]")
        demands.append((coordinate, float(weight)))

    equipment = payload.get("equipment", [])
    exclusions = payload.get("static_exclusions", [])
    if not isinstance(equipment, list) or not all(isinstance(value, str) and value for value in equipment):
        raise ValueError("equipment must be a list of non-empty strings")
    if not isinstance(exclusions, list) or not all(isinstance(value, str) and value for value in exclusions):
        raise ValueError("static_exclusions must be a list of non-empty strings")
    bodyweight = payload.get("bodyweight_supported", False)
    if not isinstance(bodyweight, bool):
        raise ValueError("bodyweight_supported must be boolean")

    return MetadataEntry(
        catalogue_key=key,
        logging_type=logging_type,
        demand_coordinates=tuple(sorted(demands)),
        equipment=tuple(sorted(equipment)),
        bodyweight_supported=bodyweight,
        static_exclusions=tuple(sorted(exclusions)),
    )


def load_catalogue(payload: Mapping[str, Any]) -> MetadataCatalogue:
    """Validate a versioned metadata manifest and return its stable content hash."""

    version = payload.get("catalogue_version")
    if not isinstance(version, str) or not version:
        raise ValueError("catalogue_version is required")
    coordinates = payload.get("coordinate_schema")
    if not isinstance(coordinates, list) or not coordinates or not all(isinstance(value, str) and value for value in coordinates):
        raise ValueError("coordinate_schema must be a non-empty string list")
    if len(coordinates) != len(set(coordinates)):
        raise ValueError("coordinate_schema has duplicate coordinates")
    entries_payload = payload.get("entries")
    if not isinstance(entries_payload, list) or not entries_payload:
        raise ValueError("entries must be a non-empty list")

    entries = tuple(_entry_from_payload(entry, set(coordinates)) for entry in entries_payload)
    keys = [entry.catalogue_key for entry in entries]
    if len(keys) != len(set(keys)):
        raise ValueError("catalogue_key must be unique")
    aliases = payload.get("seed_aliases", {})
    if not isinstance(aliases, Mapping) or not all(
        isinstance(name, str) and name and isinstance(key, str) and key in set(keys)
        for name, key in aliases.items()
    ):
        raise ValueError("seed_aliases must map non-empty seed names to declared catalogue keys")

    canonical = {
        "catalogue_version": version,
        "coordinate_schema": list(coordinates),
        "seed_aliases": dict(sorted(aliases.items())),
        "entries": [
            {
                "catalogue_key": entry.catalogue_key,
                "logging_type": entry.logging_type,
                "demand_coordinates": dict(entry.demand_coordinates),
                "equipment": list(entry.equipment),
                "bodyweight_supported": entry.bodyweight_supported,
                "static_exclusions": list(entry.static_exclusions),
            }
            for entry in sorted(entries, key=lambda item: item.catalogue_key)
        ],
    }
    return MetadataCatalogue(
        catalogue_version=version,
        coordinate_schema=tuple(coordinates),
        entries=tuple(sorted(entries, key=lambda item: item.catalogue_key)),
        seed_aliases=tuple(sorted(aliases.items())),
        metadata_hash=sha256(_canonical_json(canonical).encode("utf-8")).hexdigest(),
    )


def unanchored_coordinates(catalogue: MetadataCatalogue) -> set[str]:
    """Return declared coordinates with fewer than two independently keyed measurements."""

    keys_by_coordinate: dict[str, set[str]] = {coordinate: set() for coordinate in catalogue.coordinate_schema}
    for entry in catalogue.entries:
        for coordinate, _ in entry.demand_coordinates:
            keys_by_coordinate[coordinate].add(entry.catalogue_key)
    return {coordinate for coordinate, keys in keys_by_coordinate.items() if len(keys) < 2}
