package com.alen.distributed.problem.lock.zk.javaapi;

import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

/**
 * zk客户端
 *
 * @author alen
 * @create 2018-11-15 16:04
 **/
public class ZKClient {
    private final static String CONNECTSTRING="127.0.0.1:2181,127.0.0.1:2182,127.0.0.1:2183";
    private static int sessionTimeout=5000;
    //获取连接
    public static ZooKeeper getInstance() {
        final CountDownLatch conectStatus=new CountDownLatch(1);
        ZooKeeper zooKeeper= null;
        try {
            zooKeeper = new ZooKeeper(CONNECTSTRING, sessionTimeout, new Watcher() {
                @Override
                public void process(WatchedEvent event) {
                    if(event.getState()== Event.KeeperState.SyncConnected){
                        conectStatus.countDown();
                    }
                }
            });
            conectStatus.await();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return zooKeeper;
    }

    public static int getSessionTimeout() {
        return sessionTimeout;
    }
}

