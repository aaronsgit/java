package DisruptorSample;import com.lmax.disruptor.*;import java.text.DecimalFormat;import java.util.concurrent.LinkedBlockingQueue;/** * Created with IntelliJ IDEA. Project: mobile-tracer-web Author: Kevin Date: * 16/5/24 Time: 下午12:04 */public class CompareDemo {	final long objSize = 1 << 20;	public void testLinkedBlockQueue() throws Exception {		final LinkedBlockingQueue<TestObject> queue = new LinkedBlockingQueue<TestObject>();		Thread producer = new Thread(new Runnable() {// 生产者					@Override					public void run() {						try {							for (long i = 1; i <= objSize; i++) {								queue.add(new TestObject(i));							}						} catch (Exception e) {						}					}				});		Thread consumer = new Thread(new Runnable() {// 消费者					@Override					public void run() {						try {							TestObject readObj = null;							for (long i = 1; i <= objSize; i++) {								// do something								readObj = queue.remove();							}						} catch (Exception e) {						}					}				});		long timeStart = System.currentTimeMillis();		producer.start();		consumer.start();		consumer.join();		producer.join();		long timeEnd = System.currentTimeMillis();		DecimalFormat df = (DecimalFormat) DecimalFormat.getInstance();		System.out.println((timeEnd - timeStart) + "/" + df.format(objSize)				+ " = " + df.format(objSize / (timeEnd - timeStart) * 1000));	}	public void testRingBuffer() throws Exception {		// 创建一个单生产者的RingBuffer，EventFactory是填充缓冲区的对象工厂		final RingBuffer<TestObject> ringBuffer = RingBuffer.createSingleProducer(				new EventFactory<TestObject>() {					@Override					public TestObject newInstance() {						return new TestObject(0);					}				}, (int) objSize, new YieldingWaitStrategy());		// 创建消费者指针		final SequenceBarrier barrier = ringBuffer.newBarrier();		Thread producer = new Thread(new Runnable() {// 生产者			@Override			public void run() {				for (long i = 1; i <= objSize; i++) {					long index = ringBuffer.next();// 申请下一个缓冲区Slot					ringBuffer.get(index).setValue(i);// 对申请到的Slot赋值					ringBuffer.publish(index);// 发布，然后消费者可以读到				}			}		});		Thread consumer = new Thread(new Runnable() {// 消费者			@Override			public void run() {				TestObject readObj = null;				int readCount = 0;				long readIndex = Sequencer.INITIAL_CURSOR_VALUE;				while (readCount < objSize)// 读取objCount个元素后结束				{					try {						long nextIndex = readIndex + 1;// 当前读取到的指针+1，即下一个该读的位置						long availableIndex = barrier								.waitFor(nextIndex);// 等待直到上面的位置可读取						while (nextIndex <= availableIndex)						{							readObj = ringBuffer.get(nextIndex);// 获得Buffer中的对象							// DoSomethingAbout(readObj);							readCount++;							nextIndex++;						}						readIndex = availableIndex;// 刷新当前读取到的位置					} catch (Exception ex) {						ex.printStackTrace();					}				}			}		});		long timeStart = System.currentTimeMillis();		producer.start();		consumer.start();		consumer.join();		producer.join();		long timeEnd = System.currentTimeMillis();		DecimalFormat df = (DecimalFormat) DecimalFormat.getInstance();		System.out.println((timeEnd - timeStart) + "/" + df.format(objSize)				+ " = " + df.format(objSize / (timeEnd - timeStart) * 1000));	}	public static void main(String[] args) throws Exception {		CompareDemo compareDemo = new CompareDemo();		compareDemo.testLinkedBlockQueue();		compareDemo.testRingBuffer();	}}