package akka.workshop.example.java.actors;

import akka.actor.*;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Creator;
import akka.japi.Function;
import akka.japi.Procedure;
import akka.persistence.Persistent;
import akka.routing.ConsistentHashingRouter;
import akka.util.Timeout;
import akka.workshop.example.java.messages.*;
import akka.workshop.example.java.utils.forex.SymbolStream;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static akka.actor.SupervisorStrategy.restart;
import static akka.actor.SupervisorStrategy.resume;
import static akka.actor.SupervisorStrategy.stop;
import static akka.actor.SupervisorStrategy.escalate;
import static akka.pattern.Patterns.ask;

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
        if (message instanceof AccountStatusResponse) {
           log.info("Account status :"+message.toString());
           getSender().tell(message,getSelf());
        } else if (message instanceof AccountStatusQuery) {
            // status query
            reportAccountStatus((AccountStatusQuery) message);
        } else if (message instanceof AccountCalibration) {
            //side effect from incoming rates - don't persist to journal
            final ActorRef actorRef = getContext().getChild(((AccountCalibration) message).getName());
            //un-subscribe from rates events
            getContext().system().eventStream().unsubscribe(actorRef,SymbolStream.class);
        } else if (message instanceof AccountCreation) {
            //persist to journal
            accountCreation((AccountCreation) message);
        } else if (message instanceof DealOpening) {
            //persist to journal
            dealOpening((DealOpening) message);
        } else if (message instanceof AccountTermination) {
            terminateAccount((AccountTermination) message);
        } else if (message instanceof AccountDeposit) {
            //persist to journal
            deposit((AccountDeposit) message);
        } else {
            unhandled(message);
        }
    }

    private void terminateAccount(AccountTermination message) {
        final ActorRef actorRef = getContext().getChild(message.getName());
        getContext().system().eventStream().unsubscribe(actorRef,SymbolStream.class);
        actorRef.tell(PoisonPill.getInstance(), getSelf());
        getContext().become(shuttingDown);
    }

    private void deposit(AccountDeposit message) throws Exception {
        try {
            final ActorRef actorRef = getContext().getChild(message.getAccount());
            //send-reply message
            Timeout timeout = new Timeout(Duration.create(100, TimeUnit.MILLISECONDS));
            Future<Object> future = ask(actorRef, message, timeout);
//            Future<Object> future = ask(actorRef, Persistent.create(message), timeout);
            Object result = Await.result(future, timeout.duration());
            getSender().tell(result, getSelf());
        } catch (Exception e) {
            log.warning("Account deposit failed to to TimeoutException " +message);
            getSender().tell(e,getSelf());
        }
    }

    private void dealOpening(DealOpening message) throws Exception {
        try {
            final DealOpening dealOpening = message;
            final ActorRef actorRef = getContext().getChild(dealOpening.getAccount());
            //send-reply message
            Timeout timeout = new Timeout(Duration.create(100, TimeUnit.MILLISECONDS));
            Future<Object> future = ask(actorRef, dealOpening, timeout);
//            Future<Object> future = ask(actorRef, Persistent.create(dealOpening), timeout);
            Object result = Await.result(future, timeout.duration());
            getSender().tell(result, getSelf());
        } catch (TimeoutException e) {
            log.warning("Deal opening failed to to TimeoutException " +message);
            getSender().tell(e,getSelf());
        }
    }

    private void accountCreation(AccountCreation message) throws Exception {
        final AccountCreation accountCreation = message;
        //create account actor
        final ActorRef actorRef = getContext().actorOf(AccountProcessor.props(accountCreation), accountCreation.getName());
        //subscribe to rates events
        getContext().system().eventStream().subscribe(actorRef,SymbolStream.class);

      //validate actor alive
        //option 1: explicit blocking
        //send-reply message - verify account processor online
        Timeout timeout = new Timeout(Duration.create(1, TimeUnit.SECONDS));
        Future<Object> future = ask(actorRef, accountCreation, timeout);
//        Future<Object> future = ask(actorRef, Persistent.create(accountCreation), timeout);
        try {
            Object result = Await.result(future, timeout.duration());
            getSender().tell(result, getSelf());
        } catch (TimeoutException e) {
            log.info("Account creation verification failed - should retry policy");
            akka.pattern.Patterns.pipe(future, getContext().dispatcher()).to(getSender(),getSelf());
        }
    }

    private void reportAccountStatus(AccountStatusQuery message) throws Exception {
        final AccountStatusQuery query = message;
        final ActorRef actorRef = getContext().getChild(query.getName());
        //send-reply message
        Timeout timeout = new Timeout(Duration.create(100, TimeUnit.MILLISECONDS));
        Future<Object> future = ask(actorRef, query, timeout);
        akka.pattern.Patterns.pipe(future, getContext().dispatcher()).to(getSender(),getSelf());
    }

    private static SupervisorStrategy strategy =
      new OneForOneStrategy(10, Duration.create("1 minute"),
        new Function<Throwable, SupervisorStrategy.Directive>() {
            @Override
          public SupervisorStrategy.Directive apply(Throwable t) {
            System.out.println("TradingProcessor supervisor "+t.getMessage());
            if(t instanceof ArithmeticException ) {
                return resume();
            } else if(t instanceof IllegalActorStateException ) {
              return resume();
            } else if (t instanceof NullPointerException) {
              return restart();
            } else if (t instanceof IllegalArgumentException) {
              return stop();
            } else {
              return escalate();
            }
          }
        });

    Procedure<Object> shuttingDown = new Procedure<Object>() {
        @Override
        public void apply(Object message) {
            if (message instanceof AccountCommand) {
                getSender().tell("account service unavailable, shutting down", getSelf());
            } else if (message instanceof Terminated) {
                getContext().stop(getSelf());
            }
        }
    };

    @Override
    public SupervisorStrategy supervisorStrategy() {
      return strategy;
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