package akka.workshop.example.java.messages;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * Created by user on 12/02/14.
 */
public class DealClosed implements Serializable{
    private String account;
    private final String dealId;
    private Date closeDate;
    private BigDecimal revenue;

    public DealClosed(String id, Date closeDate, BigDecimal revenue) {
        this.dealId = id;
        this.closeDate = closeDate;
        this.revenue = revenue;
    }

    public String getDealId() {
        return dealId;
    }

    public Date getCloseDate() {
        return closeDate;
    }

    public BigDecimal getRevenue() {
        return revenue;
    }
}
