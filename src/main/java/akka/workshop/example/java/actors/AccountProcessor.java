package akka.workshop.example.java.actors;

import akka.actor.OneForOneStrategy;
import akka.actor.Props;
import akka.actor.SupervisorStrategy;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Creator;
import akka.japi.Function;
import akka.persistence.UntypedProcessor;
import akka.workshop.example.java.messages.AccountCommand;
import akka.workshop.example.java.messages.AccountCreation;
import akka.workshop.example.java.utils.forex.AccountServices;
import akka.workshop.example.java.utils.forex.Deal;
import scala.concurrent.duration.Duration;

import java.math.BigDecimal;

import static akka.actor.SupervisorStrategy.restart;
import static akka.actor.SupervisorStrategy.resume;
import static akka.actor.SupervisorStrategy.stop;
import static akka.actor.SupervisorStrategy.escalate;


public class AccountProcessor extends UntypedProcessor implements AccountServices {
    LoggingAdapter log = Logging.getLogger(getContext().system(), this);

    public AccountProcessor(AccountCreation accountCreation) {
        log.debug(accountCreation.toString());
        //todo populate accont entity
    }

    public void onReceive(Object message) throws Exception {
        log.info("received message: " + message + " from sender " + getSender().path());
        if (message instanceof AccountCommand) {
            //todo
        } else {
            unhandled(message);
        }
    }

    public static Props props(final AccountCreation accountCreation) {
        return Props.create(new Creator<AccountProcessor>() {
            private static final long serialVersionUID = 1L;

            @Override
            public AccountProcessor create() throws Exception {
                return new AccountProcessor(accountCreation);
            }
        });
    }

    @Override
    public void postStop() {
        // clean up resources here ...
        log.info("Shutting down " );
    }

    private static SupervisorStrategy strategy =
            new OneForOneStrategy(10, Duration.create("1 minute"),
                    new Function<Throwable, SupervisorStrategy.Directive>() {
                        @Override
                        public SupervisorStrategy.Directive apply(Throwable t) {
                            System.out.println("AccountProcessor supervisor " + t.getMessage());
                            //todo implement recovery policy here
                            return null;
                        }
                    });

    @Override
    public SupervisorStrategy supervisorStrategy() {
        return strategy;
    }

    @Override
    public void deposit(BigDecimal amount) {
        //todo
    }

    @Override
    public void addDeal(Deal deal) {
        //todo
    }
}