package sample.cluster.sharding;

import akka.actor.PoisonPill;
import akka.actor.ReceiveTimeout;
import akka.contrib.pattern.ShardRegion;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Procedure;
import akka.persistence.UntypedEventsourcedProcessor;
import scala.concurrent.duration.Duration;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

/**
 * Created by lior.perry on 16/06/2014.
 */
public class Counter extends UntypedEventsourcedProcessor {
    LoggingAdapter log = Logging.getLogger(getContext().system(), this);


    public static enum CounterOp implements Serializable{
        INCREMENT, DECREMENT
    }

    public static class Get implements Serializable{
        final public long counterId;

        public Get(long counterId) {
            this.counterId = counterId;
        }
    }

    public static class EntryEnvelope implements Serializable{
        final public long id;
        final public Object payload;

        public EntryEnvelope(long id, Object payload) {
            this.id = id;
            this.payload = payload;
        }
    }

    public static class CounterChanged implements Serializable {
        final public int delta;

        public CounterChanged(int delta) {
            this.delta = delta;
        }
    }

    int count = 0;

    @Override
    public void preStart() throws Exception {
        super.preStart();
        context().setReceiveTimeout(Duration.create(120, TimeUnit.SECONDS));
    }

    void updateState(CounterChanged event) {
        count += event.delta;
    }

    @Override
    public void onReceiveRecover(Object msg) {
        if (msg instanceof CounterChanged)
            updateState((CounterChanged) msg);
        else
            unhandled(msg);
    }

    @Override
    public void onReceiveCommand(Object msg) {
        log.info("Counter received message:"+msg);
        if (msg instanceof Get)
            getSender().tell(count, getSelf());

        else if (msg == CounterOp.INCREMENT)
            persist(new CounterChanged(+1), new Procedure<CounterChanged>() {
                public void apply(CounterChanged evt) {
                    updateState(evt);
                }
            });

        else if (msg == CounterOp.DECREMENT)
            persist(new CounterChanged(-1), new Procedure<CounterChanged>() {
                public void apply(CounterChanged evt) {
                    updateState(evt);
                }
            });

        else if (msg.equals(ReceiveTimeout.getInstance()))
            getContext().parent().tell(
                    new ShardRegion.Passivate(PoisonPill.getInstance()), getSelf());

        else
            unhandled(msg);
    }
}
