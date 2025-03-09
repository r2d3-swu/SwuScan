import lombok.Data;

//@AllArgsConstructor
@Data
public class Card {
    private String set;
    private String cardName;
    private String cardNumber;
    private int count;
    private Boolean isFoil;
    private String uniqueDisplayName;

    public Card(String set, String cardName, String cardNumber, int count, boolean isFoil){
        this.set = set;
        this.cardName = cardName;
        this.cardNumber = cardNumber;
        this.count = count;
        this.isFoil = isFoil;
        this.uniqueDisplayName = cardName;
    }
    public Card(String set, String cardName, String cardNumber){
        this.set = set;
        this.cardName = cardName;
        this.cardNumber = cardNumber;
        this.count = -1;
        this.isFoil = null;
        this.uniqueDisplayName = cardName;
    }
    public Card(){
        this.cardName = "";
        this.uniqueDisplayName = "";
    }
    public boolean isFoil(){
        return isFoil;
    }

    @Override
    public String toString(){
        return "["+set+", "+cardName+ ", "+cardNumber+", "+count+", "+isFoil+"]";
    }
}
