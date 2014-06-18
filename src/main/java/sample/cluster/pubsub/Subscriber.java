package sample.cluster.pubsub;

import akka.actor.ActorRef;
import akka.actor.UntypedActor;
import akka.contrib.pattern.DistributedPubSubExtension;
import akka.contrib.pattern.DistributedPubSubMediator;
import akka.event.Logging;
import akka.event.LoggingAdapter;

/**
 * Created by lior.perry on 16/06/2014.
 */
public class Subscriber extends UntypedActor {
    LoggingAdapter log = Logging.getLogger(getContext().system(), this);

    public Subscriber() {
        ActorRef mediator =
                DistributedPubSubExtension.get(getContext().system()).mediator();
        // subscribe to the topic named "content"
        mediator.tell(new DistributedPubSubMediator.Subscribe("content", getSelf()),
                getSelf());
    }

    public void onReceive(Object msg) {
        if (msg instanceof String)
            log.info("Got: {} from {}", msg , getSender().path());
        else if (msg instanceof DistributedPubSubMediator.SubscribeAck)
            log.info("subscribing");
        else
            unhandled(msg);
    }
}
