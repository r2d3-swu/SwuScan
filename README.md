# SwuScanDesktop

App for scanning cards in Star Wars Unlimited.
Will create a CSV file at output_collection.csv that can be uploaded to
swudb.com/collection -> Bulk Actions -> CSV import
If the program is restarted the csv will reset before a new card is added. 

## Install:
* Java - https://learn.microsoft.com/en-us/java/openjdk/download#openjdk-21
* Maven - https://www.baeldung.com/install-maven-on-windows-linux-mac

## Run 
(in terminal):
* mvn clean install
* mvn exec:java

If webcam not working try changing WEBCAM_ID in config.yml to something else
(0 = first cam, 1 = second cam etc.)

Using a smartphone as webcam makes it easier to use:
* Android: https://droidcam.app/
* Ios https://reincubate.com/camo/downloads/

## Develop 
* Python
* pip install requests
* pip install pillow

Note: only tested on windows

## To add new set
How add a new set
* cd scripts
* Change "SET" in and run generate_swudb_input.py
* Go to swudb.com (account with empty collection)
* Take src/main/resources/<set>_full.csv file and import bulk in swudb.com
* Download human readable collection and name it "human_readable_full_collection_<set>.csv"
* Change "SET" in and run download_images.py
* Add set to "SETS" variable in Main.java
