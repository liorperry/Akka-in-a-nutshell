package akka.workshop.example.java.messages;

import java.math.BigDecimal;

/**
 * Created by user on 12/02/14.
 */
public class AccountWithdraw extends AccountCommand {
    BigDecimal amount;

    public AccountWithdraw(String account, BigDecimal amount) {
        super(account);
        this.amount = amount;
    }

    public String getAccount() {
        return getName();
    }

    public BigDecimal getAmount() {
        return amount;
    }
}
