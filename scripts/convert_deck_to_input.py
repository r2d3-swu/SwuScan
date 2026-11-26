import json
import csv
import sys
from pathlib import Path

def json_to_csv(input_file, output_file):
    # Load JSON
    with open(input_file, "r", encoding="utf-8") as f:
        data = json.load(f)

    rows = []

    # Collect leader, secondleader, base
    for key in ["leader", "secondleader", "base"]:
        if key in data:
            card_id = data[key]["id"]
            count = data[key]["count"]
            set_name, card_number = card_id.split("_")
            rows.append([set_name, card_number, count, "False"])

    # Collect deck cards
    for card in data.get("deck", []):
        card_id = card["id"]
        count = card["count"]
        set_name, card_number = card_id.split("_")
        rows.append([set_name, card_number, count, "False"])

    # Write CSV
    with open(output_file, "w", newline="", encoding="utf-8") as f:
        writer = csv.writer(f)
        writer.writerow(["Set", "CardNumber", "Count", "IsFoil"])
        writer.writerows(rows)

if __name__ == "__main__":
    # Example usage: python script.py scratch_1.json scratch_2.csv
    if len(sys.argv) < 3:
        print("Usage: python script.py input.json output.csv")
    else:
        json_to_csv(sys.argv[1], sys.argv[2])
