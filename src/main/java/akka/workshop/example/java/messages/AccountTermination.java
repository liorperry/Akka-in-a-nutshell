package akka.workshop.example.java.messages;

/**
 * Created by user on 12/02/14.
 */
public class AccountTermination extends AccountCommand{


    public AccountTermination(String name) {
        super(name);
    }



    @Override
    public String toString() {
        return "AccountCreation{" +
                "name='" + getName() + '\'' +
                '}';
    }
}
