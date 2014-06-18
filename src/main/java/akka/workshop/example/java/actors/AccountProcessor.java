package akka.workshop.example.java.actors;

import akka.actor.*;
import akka.dispatch.Mapper;
import akka.dispatch.OnSuccess;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Creator;
import akka.japi.Function;
import akka.japi.Procedure;
import akka.pattern.Patterns;
import akka.persistence.Persistent;
import akka.persistence.UntypedProcessor;
import akka.util.Timeout;
import akka.workshop.example.java.messages.*;
import akka.workshop.example.java.utils.forex.*;
import com.google.common.base.Optional;
import scala.concurrent.Await;
import scala.concurrent.ExecutionContextExecutor;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

import static akka.dispatch.Futures.sequence;
import static akka.pattern.Patterns.ask;
import static akka.actor.SupervisorStrategy.resume;
import static akka.actor.SupervisorStrategy.restart;
import static akka.actor.SupervisorStrategy.stop;
import static akka.actor.SupervisorStrategy.escalate;


public class AccountProcessor extends UntypedProcessor implements AccountServices {
    LoggingAdapter log = Logging.getLogger(getContext().system(), this);
    private SymbolStream currentRates;
    private Account account;

    public AccountProcessor(AccountCreation accountCreation) {
        log.debug(accountCreation.toString());
        account = openAccount(accountCreation.getName(), accountCreation.getBalance());
    }

    public void onReceive(Object message) throws Exception {
        log.info("received message: "+message +" from sender " + getSender().path());
        if (message instanceof Persistent) {
            // message successfully written to journal
            Persistent persistent = (Persistent)message;
            Long sequenceNr = persistent.sequenceNr();
            log.info("Persistent message #"+sequenceNr+" arrived "+message);
            message = persistent.payload();
        }

        if (message instanceof AccountCreation) {
            getSender().tell(new AccountStatusResponse(account),getSelf());
        } else if (message instanceof SymbolStream) {
            calculateBalance((SymbolStream) message);
        } else if (message instanceof AccountStatusQuery) {
            log.info("Reporting account status "+account);
            getSender().tell(new AccountStatusResponse(account),getSelf());
        } else if (message instanceof AccountDeposit) {
            final AccountDeposit accountDeposit = (AccountDeposit) message;
            deposit(accountDeposit.getAmount());
            getSender().tell(account.getBalance(), getSelf());
        } else if (message instanceof DealOpening) {
            if(currentRates!=null) {
                openDeal((DealOpening) message);
            } else {
                log.warning("Actor didn't receive rates" + account);
                throw new IllegalActorStateException("Actor didn't receive rates");
            }
       } else if (message instanceof DealClosing) {
            if(currentRates!=null) {
                closeDeal((DealClosing) message);
            } else {
                log.warning("Actor didn't receive rates");
                throw new IllegalActorStateException("Actor didn't receive rates");
            }
        } else if (message instanceof DealClosed) {
            log.info("Revenue from deal "+((DealClosed) message).getDealId() +" : "+((DealClosed) message).getRevenue());
            terminateDeal((DealClosed) message);
        } else {
            unhandled(message);
        }
    }

    private void closeDeal(DealClosing message) {
        final ActorRef actorRef = getContext().getChild(message.getDealId());
        Optional<Deal> deal = account.getDeal(message.getDealId());
        if(deal.isPresent()) {
            SymbolPosition symbolPosition = currentRates.getSymbol(deal.get().getSymbol());
            actorRef.tell(new DealClosingWithPosition(message.getDealId(), symbolPosition, deal.get().getType(),deal.get().getBuyAmount(),message.getCloseDate()), getSelf());
        } else {
            //todo some smart stuff
            log.warning("Deal with Id "+message.getDealId() +" wasn't found in account state");
        }
    }

    private void terminateDeal(DealClosed message) {
        final ActorRef actorRef = getContext().getChild(message.getDealId());
        actorRef.tell(PoisonPill.getInstance(), getSelf());
        getContext().become(shuttingDown);
    }


    private void openDeal(DealOpening message) throws Exception {
        final DealOpening dealOpening = message;
        //create deal processor actor
        final String name = DealOpening.getName(dealOpening.getSymbol(), dealOpening.getType(), dealOpening.getOpenDate(),dealOpening.getAmount());
        final ActorRef actorRef = getContext().actorOf(Props.create(DealProcessor.class), name);
        //send-reply message
        Timeout timeout = new Timeout(Duration.create(10, TimeUnit.SECONDS));
        final DealOpeningWithPosition openingWithPosition = new DealOpeningWithPosition(dealOpening.getDealId(),currentRates.getSymbol(dealOpening.getSymbol()), dealOpening.getType(), dealOpening.getAmount(), dealOpening.getOpenDate());
        Future<Object> future = ask(actorRef, openingWithPosition, timeout);
        final Deal deal = (Deal) Await.result(future, timeout.duration());
        addDeal(deal);
        log.info("Adding deal "+deal +" to account "+account);
        getSender().tell(deal, getSelf());
    }

    private void calculateBalance(SymbolStream message) {
        log.debug(Thread.currentThread().getName() + " Received Rate Stream message: ", message.getTimestamp());
        currentRates = message;

        //send-reply message
        Timeout timeout = new Timeout(Duration.create(10, TimeUnit.SECONDS));
        final Iterable<ActorRef> children = getContext().getChildren();
        ArrayList<Future<Object>> responses = new ArrayList<>();
        for (ActorRef child : children) {
            Future<Object> future = Patterns.ask(child, currentRates, timeout);
            responses.add(future);
        }
        ExecutionContextExecutor contextExecutor = getContext().dispatcher();
        Future<Iterable<Object>> iterableFuture = sequence(responses, contextExecutor);
        // Find the sum of
        Future<BigDecimal> futureSum = iterableFuture.map(
                new Mapper<Iterable<Object>, BigDecimal>() {
                    public BigDecimal apply(Iterable<Object> ints) {
                        Iterator<Object> iterator = ints.iterator();
                        while (iterator.hasNext()) {
                            Object next = iterator.next();
                            account.updateProfitOrLoss((BigDecimal) next);
                            log.info(Thread.currentThread().getName() + "-Deal balance " + next);
                        }
                        return account.getBalance();
                    }
                }, contextExecutor
        );

        futureSum.onSuccess(new OnSuccess<BigDecimal>() {
            public void onSuccess(BigDecimal value) {
                log.info(Thread.currentThread().getName()+"-Account updated balance "+value);
            }
        }, contextExecutor);

 /*       try {
            Await.result(futureSum, Duration.apply(100, TimeUnit.MILLISECONDS));
        } catch (Exception e) {
            throw new ArithmeticException();
        }
*/
        if(account.getBalance().signum()==-1) {
            getContext().parent().tell(new AccountCalibration(account.getName()),getSelf());
        }

        //message was delivered via EventBus -
    }

    public Account openAccount(String name, int deposit) {
        return new Account(name, BigDecimal.valueOf(deposit));
    }

    @Override
    public void deposit(BigDecimal amount) {
        if(amount.signum()<0)
            throw new ArithmeticException("Deposit amount smaller than 0");
        account.addBalance(amount);
    }

    @Override
    public void addDeal(Deal deal) {
        account.addDeal(deal);
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
        log.info("Shutting down "+account);
    }

    private static SupervisorStrategy strategy =
      new OneForOneStrategy(10, Duration.create("1 minute"),
        new Function<Throwable, SupervisorStrategy.Directive>() {
          @Override
          public SupervisorStrategy.Directive apply(Throwable t) {
              System.out.println("AccountProcessor supervisor "+t.getMessage());
            if(t instanceof ArithmeticException) {
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

    @Override
    public SupervisorStrategy supervisorStrategy() {
      return strategy;
    }

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

}