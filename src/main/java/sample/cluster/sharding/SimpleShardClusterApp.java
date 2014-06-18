package sample.cluster.sharding;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.contrib.pattern.ClusterSharding;
import akka.contrib.pattern.ShardRegion;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import sample.cluster.simple.SimpleClusterListener;

public class SimpleShardClusterApp {

    public static void main(String[] args) {
        if (args.length == 0)
            startup(new String[]{"2551", "2552", "0"});
        else
            startup(args);
    }

    public static void startup(String[] ports) {
        for (String port : ports) {
            // Override the configuration of the port
            Config config = ConfigFactory.parseString("akka.remote.netty.tcp.port=" + port).withFallback(ConfigFactory.load());

            // Create an Akka system
            ActorSystem system = ActorSystem.create("ClusterSystem", config);

            // Create an actor that handles cluster domain events
            system.actorOf(Props.create(SimpleShardClusterListener.class),
                    "clusterListener");

            ClusterSharding.get(system).start("Counter", Props.create(Counter.class),
                    messageExtractor);

        }

    }

    static ShardRegion.MessageExtractor messageExtractor = new ShardRegion.MessageExtractor() {

        @Override
        public String entryId(Object message) {
            if (message instanceof Counter.EntryEnvelope)
                return String.valueOf(((Counter.EntryEnvelope) message).id);
            else if (message instanceof Counter.Get)
                return String.valueOf(((Counter.Get) message).counterId);
            else
                return null;
        }

        @Override
        public Object entryMessage(Object message) {
            if (message instanceof Counter.EntryEnvelope)
                return ((Counter.EntryEnvelope) message).payload;
            else
                return message;
        }

        @Override
        public String shardId(Object message) {
            if (message instanceof Counter.EntryEnvelope) {
                long id = ((Counter.EntryEnvelope) message).id;
                return String.valueOf(id % 10);
            } else if (message instanceof Counter.Get) {
                long id = ((Counter.Get) message).counterId;
                return String.valueOf(id % 10);
            } else {
                return null;
            }
        }

    };
}
