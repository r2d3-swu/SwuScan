import csv
from collections import OrderedDict

MAX_NR = 600
SET = "JTL"

FILE_NAME = "../src/main/resources/"+SET.lower()+"_full.csv"
def generate_full():
# Write headers only once
    with open(FILE_NAME, mode="w", newline="") as file:
        writer = csv.writer(file, delimiter=",")
        writer.writerow(["Set", "CardNumber", "Count", "IsFoil"])  # Column headers

    # Append rows one by one
    with open(FILE_NAME, mode="a", newline="") as file:
        writer = csv.writer(file, delimiter=",")
        for i in range(MAX_NR):
            nr_to_add = i + 1
            str_to_add = str(nr_to_add)
            if nr_to_add < 10:
                str_to_add = "00"+str_to_add
            elif nr_to_add < 100:
                str_to_add = "0"+str_to_add

            row_to_add = [SET,str_to_add,1,"FALSE"]
            writer.writerow(row_to_add)

    print("Rows added successfully!")



if __name__ == "__main__":
    generate_full()