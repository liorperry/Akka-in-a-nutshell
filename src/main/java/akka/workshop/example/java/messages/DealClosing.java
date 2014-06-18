package akka.workshop.example.java.messages;

import akka.workshop.example.java.utils.forex.Deal;
import akka.workshop.example.java.utils.forex.Symbol;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by user on 12/02/14.
 */
public class DealClosing extends AccountCommand{
    private final String dealId;
    private Date closeDate;

    public DealClosing(String account, String id, Date closeDate) {
        super(account);
        this.dealId = id;
        this.closeDate = closeDate;
    }

    public String getAccount() {
        return getName();
    }

    public String getDealId() {
        return dealId;
    }

    public Date getCloseDate() {
        return closeDate;
    }
}
