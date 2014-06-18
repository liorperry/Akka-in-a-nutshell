package akka.workshop.example.java.messages;

import akka.workshop.example.java.utils.forex.Deal;
import akka.workshop.example.java.utils.forex.SymbolPosition;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * Created by user on 12/02/14.
 */
public class DealOpeningWithPosition implements Serializable {
    private SymbolPosition symbol;
    private Deal.DealType type;
    private BigDecimal amount;
    private Date openDate;
    private final String dealId;

    public DealOpeningWithPosition(String id,SymbolPosition symbol, Deal.DealType type, BigDecimal amount,Date openDate) {
        this.symbol = symbol;
        this.type = type;
        this.amount = amount;
        this.openDate = openDate;
        dealId = id;
    }

    public String getDealId() {
        return dealId;
    }

    public SymbolPosition getSymbol() {
        return symbol;
    }

    public Deal.DealType getType() {
        return type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public Date getOpenDate() {
        return openDate;
    }
}
