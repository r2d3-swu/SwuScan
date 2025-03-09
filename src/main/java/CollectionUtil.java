import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class CollectionUtil {

    private static final String FULL_COLLECTION_PATH = "src/main/resources/human_readable_full_collection.csv";
    private static final String COLLECTION_PATH = "src/main/resources/human_readable_full_collection_";
    private static final String OUTPUT_COLLECTION_PATH = "output_collection.csv";
    private static final String csvSeparator = ",";

    private final HashMap<String,Card> fullCollectionMapNormal = new HashMap<>();
    private final HashMap<String,Card> fullCollectionMapHyperspace = new HashMap<>();
    private final HashMap<String,Card> fullCollectionMapShowcase = new HashMap<>();

    private HashMap<String,HashMap<String,Card>> allCollectionmapNormal = new HashMap<>();
    private HashMap<String,HashMap<String,Card>> allCollectionmapHyperspace = new HashMap<>();

    public CollectionUtil(){
        Main.PLAYABLE_SETS.forEach(set -> {
            List<HashMap<String, Card>> list = initCollection(COLLECTION_PATH+set.toLowerCase()+".csv");
            allCollectionmapNormal.put(set,list.get(0));
            allCollectionmapHyperspace.put(set,list.get(1));
        });
    }

    private List<HashMap<String, Card>> initCollection(String path){
        HashMap<String,Card> fullCollectionMapNormal = new HashMap<>();
        HashMap<String,Card> fullCollectionMapHyperspace = new HashMap<>();


        //load full collection from membory

        String line = "";
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            br.readLine();
            while ((line = br.readLine()) != null) {
                // Use comma as separator
                String[] values = line.split(csvSeparator);

                // Assuming the CSV file has two columns: key, value
                if (values.length >= 7) {
                    String cardName = values[0];
                    String cardSet = values[1];
                    String cardNumber = values[2];
                    String variantType = values[3];
                    //CardName,Set,CardNumber,VariantType,Rarity,Count,IsFoil
                    //Gar Saxon,SHD,001,Normal,Common,3,False
                    if(variantType.equals("Normal")){
                        Card cardToAdd = new Card(cardSet,cardName,cardNumber);
                        Card existingCard = fullCollectionMapNormal.get(cardName);
                        if(fullCollectionMapNormal.get(cardName) != null){
                            existingCard.setUniqueDisplayName(existingCard.getCardName() + " (Leader)");
                            cardToAdd.setUniqueDisplayName(cardToAdd.getCardName() + " (Unit)");
                            fullCollectionMapNormal.put(existingCard.getUniqueDisplayName(), existingCard);
                        }
                        fullCollectionMapNormal.put(cardToAdd.getUniqueDisplayName(), cardToAdd);
                    } else if (variantType.equals("Hyperspace")){
                        Card cardToAdd = new Card(cardSet,cardName,cardNumber);
                        Card existingCard = fullCollectionMapHyperspace.get(cardName);
                        if(fullCollectionMapHyperspace.get(cardName) != null){
                            existingCard.setUniqueDisplayName(existingCard.getCardName() + " (Leader)");
                            cardToAdd.setUniqueDisplayName(cardToAdd.getCardName() + " (Unit)");
                            fullCollectionMapHyperspace.put(existingCard.getUniqueDisplayName(), existingCard);
                        }
                        fullCollectionMapHyperspace.put(cardToAdd.getUniqueDisplayName(), cardToAdd);
                    }
                }
            }
            return List.of(fullCollectionMapNormal, fullCollectionMapHyperspace);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void saveToCsv(List<Card> cards){

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(OUTPUT_COLLECTION_PATH))) {
            bw.write(String.join(csvSeparator,"Set","CardNumber","Count","IsFoil"));
            bw.newLine();
            for (Card card : cards) {
                String isFoil = String.valueOf(card.isFoil());
                String isFoilUpperCase = isFoil.substring(0,1).toUpperCase()+isFoil.substring(1);
                bw.write(String.join(csvSeparator,card.getSet(),card.getCardNumber(),String.valueOf(card.getCount()),isFoilUpperCase));
                bw.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    public List<String> getCollectionNames(){
        return new ArrayList<>(fullCollectionMapNormal.keySet());
    }
    public List<Card> getCollectionCards(){
        return new ArrayList<>(fullCollectionMapNormal.values());
    }

    public Card getCardFromName(String name){
        return fullCollectionMapNormal.get(name);
    }
    public Card getHyperspaceCardFromName(String name, String set){
        if(set.equals("ALL")){
            for(String s : Main.PLAYABLE_SETS){
                var x = allCollectionmapHyperspace.get(s).get(name);
                if(x!=null){
                    return x;
                }
            }
            return null;
        } else {
            return allCollectionmapHyperspace.get(set).get(name);
        }
    }
    public Card getShowCaseCardFromName(String name){
        return fullCollectionMapShowcase.get(name);
    }
}
