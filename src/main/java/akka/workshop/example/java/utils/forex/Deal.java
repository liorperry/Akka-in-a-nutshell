package akka.workshop.example.java.utils.forex;

import akka.workshop.example.java.messages.DealOpening;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * Created by user on 10/02/14.
 */
public class Deal implements Serializable{
    private String id;
    private SymbolPosition openPosition;
    private Date openDate;
    private DealType type;
    private BigDecimal buyAmount;
    //state
    private BigDecimal dealValue;

    public enum DealType {
        BUY,SELL
    }

    public Deal(SymbolPosition openPosition, BigDecimal buyAmount, DealType type, Date openDate) {
        this.openDate = openDate;
        this.openPosition = openPosition;
        this.buyAmount = buyAmount;
        this.type = type;
        id = DealOpening.getName(openPosition.getSymbol(), type,openDate, buyAmount);
    }

    public SymbolPosition getOpenPosition() {
        return openPosition;
    }

    public BigDecimal getBuyAmount() {
        return buyAmount;
    }

    public BigDecimal getDealValue() {
        return dealValue;
    }

    public DealType getType() {
        return type;
    }

    public String getId() {
        return id;
    }

    public Symbol getSymbol() {
        return openPosition.getSymbol();
    }

    public static BigDecimal calculateValue(Deal deal,SymbolPosition currentPosition) {
        BigDecimal delta = deal.openPosition.getValue().subtract(currentPosition.getValue());
        if (deal.type != DealType.BUY) {
            delta = currentPosition.getValue().subtract(deal.openPosition.getValue());
        }
        deal.dealValue = delta.multiply(deal.buyAmount);
        return deal.dealValue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Deal deal = (Deal) o;

        if (!id.equals(deal.id)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "Deal{" +
                "id='" + id + '\'' +
                ", openPosition=" + openPosition +
                ", openDate=" + openDate +
                ", type=" + type +
                ", buyAmount=" + buyAmount +
                '}';
    }
}
