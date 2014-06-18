package akka.workshop.example.java.utils.forex;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * Created by lior.perry on 15/06/2014.
 */
public interface AccountView extends Serializable{
    String getName();

    BigDecimal getBalance();

    List<Deal> getDeals();
}
