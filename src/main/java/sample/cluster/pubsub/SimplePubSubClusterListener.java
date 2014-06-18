package sample.cluster.pubsub;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.cluster.Cluster;
import akka.cluster.ClusterEvent;
import akka.cluster.ClusterEvent.MemberEvent;
import akka.cluster.ClusterEvent.MemberRemoved;
import akka.cluster.ClusterEvent.MemberUp;
import akka.cluster.ClusterEvent.UnreachableMember;
import akka.cluster.Member;
import akka.cluster.MemberStatus;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Creator;
import akka.workshop.example.java.messages.AccountCreation;

public class SimplePubSubClusterListener extends UntypedActor {
    LoggingAdapter log = Logging.getLogger(getContext().system(), this);
    Cluster cluster = Cluster.get(getContext().system());
    ActorRef subscriber;
    ActorRef publisher;
    private String name;

    public SimplePubSubClusterListener(String name) {
        this.name = name;
    }

    //subscribe to cluster changes
    @Override
    public void preStart() {
        //#subscribe
        cluster.subscribe(getSelf(), ClusterEvent.initialStateAsEvents(),
                MemberEvent.class, UnreachableMember.class,ClusterEvent.class);
        //#subscribe
        subscriber = getContext().system().actorOf(Props.create(Subscriber.class), "subscriber");
        publisher = getContext().system().actorOf(Props.create(Publisher.class), "publisher");
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
            // after a while the subscriptions are replicated
            publisher.tell("hello", null);
            //another node
        } else if (message instanceof ClusterEvent.CurrentClusterState) {
            ClusterEvent.CurrentClusterState state = (ClusterEvent.CurrentClusterState) message;
            for (Member member : state.getMembers()) {
                if (member.status().equals(MemberStatus.up())) {
                    log.info("Member "+member.address() +" is up");
                }
            }
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

    public static Props props(final String name) {
        return Props.create(new Creator<SimplePubSubClusterListener>() {
            private static final long serialVersionUID = 1L;

            @Override
            public SimplePubSubClusterListener create() throws Exception {
                return new SimplePubSubClusterListener(name);
            }
        });
    }
}
