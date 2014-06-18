package akka.workshop.example.java.messages;

import java.io.Serializable;

/**
 * Created by user on 12/02/14.
 */
public abstract class AccountCommand implements Serializable{
    private String name;

    public AccountCommand(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "AccountCommand{" +
                "name='" + name + '\'' +
                '}';
    }
}
