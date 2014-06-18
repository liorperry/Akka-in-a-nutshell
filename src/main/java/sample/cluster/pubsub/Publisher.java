package sample.cluster.pubsub;

import akka.actor.ActorRef;
import akka.actor.UntypedActor;
import akka.contrib.pattern.DistributedPubSubExtension;
import akka.contrib.pattern.DistributedPubSubMediator;

/**
 * Created by lior.perry on 16/06/2014.
 */
public class Publisher extends UntypedActor {

    // activate the extension
    ActorRef mediator = DistributedPubSubExtension.get(getContext().system()).mediator();

    public void onReceive(Object msg) {
        if (msg instanceof String) {
            String in = (String) msg;
            String out = in.toUpperCase();
            mediator.tell(new DistributedPubSubMediator.Publish("content", out),
                    getSelf());
        } else {
            unhandled(msg);
        }
    }
}
