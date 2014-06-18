package akka.workshop.example.java.utils.forex;

import java.math.BigDecimal;

/**
 * Created by user on 12/02/14.
 */
public interface AccountServices {
    public void deposit(BigDecimal amount);
    public void addDeal(Deal deal);
}
