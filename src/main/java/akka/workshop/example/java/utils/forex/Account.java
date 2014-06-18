package akka.workshop.example.java.utils.forex;

import com.google.common.base.Optional;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by user on 10/02/14.
 */
public class Account implements AccountView {
    String name;
    //state
    BigDecimal balance;
    //state
    List<Deal> deals;


    public Account(String name) {
        this.name=name;
    }
    public Account(String name, int deposit) {
        this(name, BigDecimal.valueOf(deposit));
    }

    public Account(String name, BigDecimal deposit) {
        this(name);
        this.balance = deposit;
        deals = new ArrayList<>();
    }

    @Override
    public String getName() {
        return name;
    }

    public void addBalance(BigDecimal amount) {
        balance = balance.add(amount);
    }

    public void addDeal(Deal deal) {
        deals.add(deal);
    }

    @Override
    public BigDecimal getBalance() {
        return balance;
    }

    @Override
    public List<Deal> getDeals() {
        return deals;
    }

    public void updateProfitOrLoss(BigDecimal currentValue) {
        this.balance = this.balance.add(currentValue);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Account account = (Account) o;

        if (name != null ? !name.equals(account.name) : account.name != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "Account{" +
                "name='" + name + '\'' +
                ", balance=" + balance +
                ", deals=" + deals +
                '}';
    }

    public Optional<Deal> getDeal(String dealId) {
        for (Deal deal : deals) {
            if(deal.getId().equals(dealId))
                return Optional.of(deal);
        }
        return Optional.absent();
    }
}
