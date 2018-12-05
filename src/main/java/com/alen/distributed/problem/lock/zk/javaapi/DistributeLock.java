package com.alen.distributed.problem.lock.zk.javaapi;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Random;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * 锁，提供获取锁和解锁的方法
 * <p>
 * 基本思路如下：
 * 1、在你指定的节点下创建一个锁目录lock；
 * 2、线程X进来获取锁在lock目录下，并创建临时有序节点；
 * 3、线程A获取lock目录下所有子节点，并获取比自己小的兄弟节点，如果不存在比自己小的节点，说明当前线程序号最小，顺利获取锁；
 * 4、此时线程Y进来创建临时节点并获取兄弟节点 ，判断自己是否为最小序号节点，发现不是，于是设置监听（watch）比自己小的节点（这里是为了发生上面说的羊群效应）；
 * 5、线程X执行完逻辑，删除自己的节点，线程Y监听到节点有变化，进一步判断自己是已经是最小节点，顺利获取锁。
 *
 * @author alen
 * @create 2018-11-15 15:58
 **/
public class DistributeLock {
    private static final Logger log = LoggerFactory.getLogger(DistributeLock.class);
    private static final String ROOT_LOCKS = "/LOCKS";//根节点
    private final static byte[] data = {1, 2}; //节点的数据
    private ZooKeeper zooKeeper;
    private int sessionTimeout; //会话超时时间
    private String lockID; //记录锁节点id
    private CountDownLatch countDownLatch = new CountDownLatch(1);
    static {
        try {
            ZKClient.getInstance().create(ROOT_LOCKS, "".getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
        } catch (KeeperException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public DistributeLock() throws IOException, InterruptedException {
        this.zooKeeper = ZKClient.getInstance();
        this.sessionTimeout = ZKClient.getSessionTimeout();
    }

    //获取锁的方法
    public boolean lock() {
        try {
            //1.通步创建/LOCKS节点下开放权限的临时顺序节点
            lockID = zooKeeper.create(ROOT_LOCKS + "/", data, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
            log.info(Thread.currentThread().getName() + "->成功创建了lock节点[" + lockID + "], 开始去竞争锁");
            //获取根节点下的所有子节点
            List<String> childrenNodes = zooKeeper.getChildren(ROOT_LOCKS, true);
            //排序，从小到大
            SortedSet<String> sortedSet = new TreeSet<String>();
            for (String children : childrenNodes) {
                sortedSet.add(ROOT_LOCKS + "/" + children);
            }
            //2.拿到最小的节点
            String first = sortedSet.first();
            if (lockID.equals(first)) {
                //表示当前就是最小的节点
                log.info(Thread.currentThread().getName() + "->成功获得锁，lock节点为:[" + lockID + "]");
                return true;
            }
            SortedSet<String> lessThanLockId = sortedSet.headSet(lockID);//headSet(E e)//e之前的元素，不包括e
            //3.注册它上一个节点的监听
            if (!lessThanLockId.isEmpty()) {
                //拿到比当前LOCKID这个几点更小的上一个节点
                String prevLockID = lessThanLockId.last();
                //exists()方法用于监控节点变化，仅仅监控对应节点的一次数据变化，无论是数据修改还是删除！
                zooKeeper.exists(prevLockID, new LockWatcher(countDownLatch));
                countDownLatch.await(sessionTimeout, TimeUnit.MILLISECONDS);
                //上面这段代码意味着如果会话超时或者节点被删除（释放）了
                log.info(Thread.currentThread().getName() + " 成功获取锁：[" + lockID + "]");
            }
            return true;
        } catch (KeeperException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 解锁，删除节点
     * @return
     */
    public boolean unlock() {
        log.info(Thread.currentThread().getName() + "->开始释放锁:[" + lockID + "]");
        try {
            zooKeeper.delete(lockID, -1);
            log.info("节点[" + lockID + "]成功被删除");
            return true;
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (KeeperException e) {
            e.printStackTrace();
        }
        return false;
    }


    public static void main(String[] args) {
        final CountDownLatch latch = new CountDownLatch(10);
        Random random = new Random();
        for (int i = 0; i < 10; i++) {
            new Thread(() -> {
                DistributeLock lock = null;
                try {
                    lock = new DistributeLock();
                    latch.countDown();
                    latch.await();
                    lock.lock();
                    Thread.sleep(random.nextInt(500));
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    if (lock != null) {
                        lock.unlock();
                    }
                }
            }).start();
        }
    }
}

