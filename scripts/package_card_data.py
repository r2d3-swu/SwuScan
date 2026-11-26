#!/usr/bin/env python3
"""Create the normal-only card data archive consumed by SwuScan Mobile."""

from __future__ import annotations

import argparse
import csv
import io
import json
import re
import sys
import zipfile
from pathlib import Path


SETS = ("SOR", "SHD", "TWI", "JTL", "LOF", "SEC", "LAW", "ASH", "HMW")
ROOT = Path(__file__).resolve().parents[1]
RESOURCE_ROOT = ROOT / "src" / "main" / "resources"
CSV_ROOT = RESOURCE_ROOT
IMAGE_ROOT = RESOURCE_ROOT / "images"


class BundleError(Exception):
    """Raised when source data cannot form a complete, valid bundle."""


def card_number(row: dict[str, str], set_code: str, line_number: int) -> str:
    value = (row.get("CardNumber") or "").strip()
    if not value.isdigit() or len(value) > 3:
        raise BundleError(
            f"{set_code} CSV line {line_number}: invalid CardNumber {value!r}"
        )
    if (row.get("Set") or "").strip() != set_code:
        raise BundleError(
            f"{set_code} CSV line {line_number}: Set must be {set_code!r}"
        )
    if not (row.get("CardName") or "").strip():
        raise BundleError(f"{set_code} CSV line {line_number}: CardName is empty")
    return value.zfill(3)


def load_normal_rows(set_code: str) -> tuple[list[str], list[dict[str, str]], set[str]]:
    csv_path = CSV_ROOT / f"human_readable_full_collection_{set_code.lower()}.csv"
    if not csv_path.is_file():
        raise BundleError(f"Missing set CSV: {csv_path.relative_to(ROOT)}")

    with csv_path.open("r", encoding="utf-8-sig", newline="") as source:
        reader = csv.DictReader(source)
        fieldnames = reader.fieldnames
        if not fieldnames:
            raise BundleError(f"CSV has no header: {csv_path.relative_to(ROOT)}")
        required = {"CardName", "Set", "CardNumber", "VariantType"}
        missing_columns = sorted(required.difference(fieldnames))
        if missing_columns:
            raise BundleError(
                f"{csv_path.relative_to(ROOT)} is missing columns: "
                + ", ".join(missing_columns)
            )

        rows: list[dict[str, str]] = []
        numbers: set[str] = set()
        for line_number, row in enumerate(reader, start=2):
            if row.get("VariantType") != "Normal":
                continue
            number = card_number(row, set_code, line_number)
            if number in numbers:
                raise BundleError(
                    f"{set_code} has more than one Normal row for card number {number}"
                )
            numbers.add(number)
            rows.append(row)

    if not rows:
        raise BundleError(f"No Normal cards found in {csv_path.relative_to(ROOT)}")
    return fieldnames, rows, numbers


def find_normal_images(set_code: str, card_numbers: set[str]) -> list[Path]:
    image_dir = IMAGE_ROOT / set_code
    if not image_dir.is_dir():
        raise BundleError(f"Missing image directory: {image_dir.relative_to(ROOT)}")

    images_by_number: dict[str, list[Path]] = {}
    for image_path in image_dir.iterdir():
        if not image_path.is_file() or image_path.suffix.lower() != ".jpg":
            continue
        suffix = image_path.stem.rsplit("_", 1)[-1]
        if suffix.isdigit():
            images_by_number.setdefault(suffix.zfill(3), []).append(image_path)

    selected: list[Path] = []
    errors: list[str] = []
    for number in sorted(card_numbers):
        matches = images_by_number.get(number, [])
        if not matches:
            errors.append(f"{set_code} {number}: no matching image")
        elif len(matches) > 1:
            names = ", ".join(sorted(path.name for path in matches))
            errors.append(f"{set_code} {number}: multiple matching images ({names})")
        else:
            selected.append(matches[0])

    if errors:
        raise BundleError("\n".join(errors))
    return selected


def render_csv(fieldnames: list[str], rows: list[dict[str, str]]) -> str:
    output = io.StringIO(newline="")
    writer = csv.DictWriter(output, fieldnames=fieldnames, lineterminator="\n")
    writer.writeheader()
    writer.writerows(rows)
    return output.getvalue()


def validate_supported_sets() -> None:
    main_path = ROOT / "src" / "main" / "java" / "Main.java"
    if not main_path.is_file():
        raise BundleError(f"Missing desktop set declaration: {main_path.relative_to(ROOT)}")

    main_source = main_path.read_text(encoding="utf-8")
    declaration = re.search(
        r"\bSETS\s*=\s*List\.of\((.*?)\);", main_source, flags=re.DOTALL
    )
    if declaration is None:
        raise BundleError("Could not read the supported set list from Main.java")

    declared_sets = tuple(re.findall(r'"([A-Z]{3})"', declaration.group(1)))
    if declared_sets[:1] == ("ALL",):
        declared_sets = declared_sets[1:]
    if declared_sets != SETS:
        raise BundleError(
            "Packager set list does not match Main.java: "
            f"packager={SETS}, desktop={declared_sets}"
        )


def create_bundle(source_commit: str, output_path: Path) -> dict[str, int]:
    if not re.fullmatch(r"(?:[0-9a-f]{40}|[0-9a-f]{64})", source_commit):
        raise BundleError("source commit must be a full 40- or 64-character Git SHA")

    validate_supported_sets()
    output_path.parent.mkdir(parents=True, exist_ok=True)
    manifest = {
        "formatVersion": 1,
        "sourceRepository": "r2d3-swu/SwuScan",
        "sourceBranch": "main",
        "sourceCommit": source_commit,
        "variantPolicy": "Normal",
        "sets": [],
    }
    counts: dict[str, int] = {}
    prepared_sets = []
    for set_code in SETS:
        fieldnames, rows, numbers = load_normal_rows(set_code)
        images = find_normal_images(set_code, numbers)
        prepared_sets.append((set_code, fieldnames, rows, images))

    with zipfile.ZipFile(
        output_path, mode="w", compression=zipfile.ZIP_DEFLATED, compresslevel=6
    ) as archive:
        for set_code, fieldnames, rows, images in prepared_sets:
            cards_path = f"sets/{set_code}/cards.csv"
            images_directory = f"sets/{set_code}/images/"
            manifest["sets"].append(
                {
                    "code": set_code,
                    "cardsCsv": cards_path,
                    "imagesDirectory": images_directory,
                }
            )
            archive.writestr(cards_path, render_csv(fieldnames, rows))
            for image_path in images:
                archive.write(
                    image_path,
                    arcname=f"{images_directory}{image_path.name}",
                    compress_type=zipfile.ZIP_STORED,
                )
            counts[set_code] = len(rows)

        archive.writestr(
            "manifest.json",
            json.dumps(manifest, indent=2, ensure_ascii=False) + "\n",
        )

    return counts


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source-commit", required=True)
    parser.add_argument("--output", required=True, type=Path)
    arguments = parser.parse_args()

    try:
        counts = create_bundle(arguments.source_commit, arguments.output)
    except (BundleError, OSError, csv.Error, zipfile.BadZipFile) as error:
        print(f"Card data bundle validation failed: {error}", file=sys.stderr)
        return 1

    total = sum(counts.values())
    details = ", ".join(f"{set_code}={count}" for set_code, count in counts.items())
    print(f"Created {arguments.output} with {total} Normal cards ({details})")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
