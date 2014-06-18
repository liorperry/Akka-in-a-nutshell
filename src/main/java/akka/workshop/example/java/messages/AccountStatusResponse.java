package akka.workshop.example.java.messages;

import akka.workshop.example.java.utils.forex.AccountView;

/**
 * Created by user on 12/02/14.
 */
public class AccountStatusResponse extends AccountCommand {
    private final AccountView accountView;
    private final long timestamp;

    public AccountStatusResponse(AccountView accountView) {
        super(accountView.getName());
        this.accountView = accountView;
        timestamp = System.currentTimeMillis();
    }

    public AccountView getAccount() {
        return accountView;
    }

    @Override
    public String toString() {
        return "AccountStatusResponse{" +
                "accountView=" + accountView +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AccountStatusResponse that = (AccountStatusResponse) o;

        if (timestamp != that.timestamp) return false;
        if (!accountView.equals(that.accountView)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = accountView.hashCode();
        result = 31 * result + (int) (timestamp ^ (timestamp >>> 32));
        return result;
    }
}
