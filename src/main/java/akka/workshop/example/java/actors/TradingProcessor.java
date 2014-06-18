package akka.workshop.example.java.actors;

import akka.actor.*;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Creator;
import akka.routing.ConsistentHashingRouter;
import akka.workshop.example.java.messages.AccountCommand;

public class TradingProcessor extends UntypedActor {
    LoggingAdapter log = Logging.getLogger(getContext().system(), this);

    public static final ConsistentHashingRouter.ConsistentHashMapper hashMapper = new ConsistentHashingRouter.ConsistentHashMapper() {
       @Override
       public Object hashKey(Object message) {
         if (message instanceof AccountCommand) {
           return ((AccountCommand) message).getName();
         } else {
           return null;
         }
       }
     };

    public void onReceive(Object message) throws Exception {
        log.debug(Thread.currentThread().getName() + " TradingProcessor:[ "+getSelf().path() +"] " + message);
        if (message instanceof AccountCommand) {
        //todo stuff here
        } else {
            unhandled(message);
        }
    }

    public static Props props(final int magicNumber) {
        return Props.create(new Creator<TradingProcessor>() {
            private static final long serialVersionUID = 1L;

            @Override
            public TradingProcessor create() throws Exception {
                return new TradingProcessor();
            }
        });
    }

}