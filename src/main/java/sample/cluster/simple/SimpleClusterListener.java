package sample.cluster.simple;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.UntypedActor;
import akka.cluster.Cluster;
import akka.cluster.ClusterEvent;
import akka.cluster.ClusterEvent.MemberEvent;
import akka.cluster.ClusterEvent.MemberUp;
import akka.cluster.ClusterEvent.MemberRemoved;
import akka.cluster.ClusterEvent.UnreachableMember;
import akka.cluster.Member;
import akka.cluster.MemberStatus;
import akka.contrib.pattern.ClusterSingletonProxy;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.workshop.example.java.messages.AccountCreation;

import java.util.*;

public class SimpleClusterListener extends UntypedActor {
    LoggingAdapter log = Logging.getLogger(getContext().system(), this);
    Cluster cluster = Cluster.get(getContext().system());
    ActorRef singleton;

    //subscribe to cluster changes
    @Override
    public void preStart() {
        //#subscribe
        cluster.subscribe(getSelf(), ClusterEvent.initialStateAsEvents(),
                MemberEvent.class, UnreachableMember.class);
        //#subscribe
    }

    //re-subscribe when restart
    @Override
    public void postStop() {
        cluster.unsubscribe(getSelf());
    }

    @Override
    public void onReceive(Object message) {
        if (message instanceof MemberUp) {
            MemberUp mUp = (MemberUp) message;
            log.info("Member is Up: {}", mUp.member());
            if(singleton==null)
                singleton = getContext().system().actorOf(ClusterSingletonProxy.defaultProps("user/singleton/consumer", "worker"), "consumerProxy");

            singleton.tell(new AccountCreation("Jhone", 100), getSender());
        } else if (message instanceof UnreachableMember) {
            UnreachableMember mUnreachable = (UnreachableMember) message;
            log.info("Member detected as unreachable: {}", mUnreachable.member());

        } else if (message instanceof MemberRemoved) {
            MemberRemoved mRemoved = (MemberRemoved) message;
            log.info("Member is Removed: {}", mRemoved.member());

        } else if (message instanceof MemberEvent) {
            // ignore

        } else {
            unhandled(message);
        }

    }
}
