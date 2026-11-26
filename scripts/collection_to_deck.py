#!/usr/bin/env python3
"""Convert output_collection.csv into a deck JSON based on sample_deck.json."""

import argparse
import csv
import json
from collections import Counter
from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parent.parent


def collection_to_deck(collection_path: Path, sample_path: Path, output_path: Path) -> None:
    with sample_path.open(encoding="utf-8") as sample_file:
        sample = json.load(sample_file)

    counts = Counter()
    with collection_path.open(newline="", encoding="utf-8-sig") as collection_file:
        reader = csv.DictReader(collection_file)
        required = {"Set", "CardNumber", "Count"}
        missing = required.difference(reader.fieldnames or [])
        if missing:
            raise ValueError(f"CSV is missing required columns: {', '.join(sorted(missing))}")

        for line_number, row in enumerate(reader, start=2):
            set_code = (row.get("Set") or "").strip()
            card_number = (row.get("CardNumber") or "").strip()
            count_text = (row.get("Count") or "").strip()
            if not set_code or not card_number or not count_text:
                raise ValueError(f"Missing Set, CardNumber, or Count on CSV line {line_number}")
            try:
                count = int(count_text)
            except ValueError as error:
                raise ValueError(f"Invalid Count on CSV line {line_number}: {count_text!r}") from error
            if count < 0:
                raise ValueError(f"Count cannot be negative on CSV line {line_number}")
            if count:
                counts[f"{set_code}_{card_number}"] += count

    result = {
        "metadata": sample["metadata"],
        "leader": sample["leader"],
        "base": sample["base"],
        "deck": [{"id": card_id, "count": count} for card_id, count in sorted(counts.items())],
        "sideboard": [],
    }

    output_path.parent.mkdir(parents=True, exist_ok=True)
    with output_path.open("w", encoding="utf-8") as output_file:
        json.dump(result, output_file, indent=2, ensure_ascii=False)
        output_file.write("\n")


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("collection", nargs="?", type=Path, default=PROJECT_ROOT / "output_collection.csv")
    parser.add_argument("--sample", type=Path, default=PROJECT_ROOT / "sample_deck.json")
    parser.add_argument("--output", type=Path, default=PROJECT_ROOT / "output_deck.json")
    args = parser.parse_args()
    collection_to_deck(args.collection, args.sample, args.output)
    print(f"Wrote {args.output}")


if __name__ == "__main__":
    main()
