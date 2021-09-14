import org.apache.zookeeper.*;


import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class leaderElection implements Watcher {
    private static final String ZOOKEEPER_ADDRESS = "localhost:2181";
    private static final int SESSION_TIMEOUT = 3000;
    private static final String ELECTION_NAMESPACE = "/election";
    private ZooKeeper zooKeeper;
    private String currentZnodeName;

    // NOTE - Don't forget to create the /election ZNode
    public static void main(String[] arg) throws IOException, InterruptedException, KeeperException {
        leaderElection leaderElection = new leaderElection();

        leaderElection.connectToZookeeper();
        leaderElection.volunteerForLeadership();
        leaderElection.electLeader();
        leaderElection.run();
        leaderElection.close();
        System.out.println("no more zookeeper, exiting application!~");
    }

    public void volunteerForLeadership() throws KeeperException, InterruptedException {
        String znodePrefix = ELECTION_NAMESPACE + "/c_";
        String znodeFullPath = zooKeeper.create(znodePrefix, new byte[]{}, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);

        System.out.println("znode name " + znodeFullPath);
        this.currentZnodeName = znodeFullPath.replace("/election/", "");
    }


    public void electLeader() throws InterruptedException, KeeperException {
        List<String> children = zooKeeper.getChildren(ELECTION_NAMESPACE, false);

        Collections.sort(children);
        String smallestChild = children.get(0);

        if (smallestChild.equals(currentZnodeName)) {
            System.out.println("I am the LEADERRRR");
            return;
        }
        System.out.println("I am no leader, " + smallestChild + " is your leader");
    }

    //connecting to zookeeper function which takes our private parameters.
    public void connectToZookeeper() throws IOException {
        this.zooKeeper = new ZooKeeper(ZOOKEEPER_ADDRESS, SESSION_TIMEOUT, this);
    }

    // a run method that waits
    public void run() throws InterruptedException {
        synchronized (zooKeeper){
            zooKeeper.wait();
        }
    }

    public void close() throws InterruptedException {
        zooKeeper.close();
    }

    // the process method is called by the zookeeper library when a separate new event is called by zookeeper
    @Override
    public void process(WatchedEvent watchedEvent) {
        switch (watchedEvent.getType()){
            case None:
                if(watchedEvent.getState() == Event.KeeperState.SyncConnected){
                    System.out.println("Successfully Connected to Zookeeper!!!!");

                } else {
                    synchronized (zooKeeper){
                        System.out.println("Disconnecting from zookeeper event");
                        zooKeeper.notifyAll();
                    }
                }
        }
    }
}
