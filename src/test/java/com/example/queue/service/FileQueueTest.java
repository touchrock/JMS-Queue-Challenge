package com.example.queue.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.example.queue.service.Clock;
import com.example.queue.service.FileQueueService;
import com.example.queue.service.exception.QueueNotFoundException;

public class FileQueueTest {
	
	private static final File TEST_DIR = new File("src/test/java/FileQueueTestDir");
	
	@Before
	public void setUp() throws IOException {
		
		//Reseting test directory
		if (!TEST_DIR.mkdir()) {
			delete(TEST_DIR);
			TEST_DIR.mkdir();
		}
	}
	
	@Test
	public void testPushAndPull() throws QueueNotFoundException, InterruptedException, IOException {
		
		System.setProperty(FileQueueService.TIMER_LENGTH_PROP, "30000");
		
		String queueName = "test";
		String message = "This is a test";
		
		FileQueueService service = new FileQueueService(TEST_DIR, new Clock());
		
		service.push(queueName, message);
		
		assertEquals(message, service.pull(queueName));
	}
	
	@Test
	public void testPushAndPullTwoQueues() throws QueueNotFoundException, InterruptedException, IOException {
		
		System.setProperty(FileQueueService.TIMER_LENGTH_PROP, "30000");
		
		String queue1 = "test";
		String message1 = "First message";
		
		String queue2 = "test2";
		String message2 = "Second message";
		
		FileQueueService service = new FileQueueService(TEST_DIR, new Clock());
		
		service.push(queue1, message1);
		service.push(queue2, message2);
		
		assertEquals(message1, service.pull(queue1));
		assertEquals(message2, service.pull(queue2));
	}
	
	/**
	 * The test will check the reliability behavior of the queue service.
	 *  The timer will have no delay, and the message will become visible again after the first pull.
	 *  Thus, a second pull will return the first message.
	 * @throws QueueNotFoundException
	 * @throws InterruptedException 
	 * @throws IOException 
	 */
	@Test
	public void testPullMoreThanTimer() throws QueueNotFoundException, InterruptedException, IOException {
		
		long timerLength = 30000;
		System.setProperty(FileQueueService.TIMER_LENGTH_PROP, String.valueOf(timerLength));
		
		String queueName = "test";
		String message = "This is a test";
		
		long time = System.currentTimeMillis();
		
		Clock clock = mock(Clock.class);
		when(clock.getTimeInNano()).thenReturn(time);
		
		FileQueueService service = new FileQueueService(TEST_DIR, clock);
		
		service.push(queueName, message);
		
		String message1 = service.pull(queueName);
		
		//Return double the timer length when clock time is called. Causing message to become visible again
		when(clock.getTimeInNano()).thenReturn(time + (timerLength * 2));
		String message2 = service.pull(queueName);
		
		assertEquals(message1, message2);
	}
	
	@Test
	public void testPullLessThanTimer() throws QueueNotFoundException, InterruptedException, IOException {
		
		long timerLength = 30000;
		System.setProperty(FileQueueService.TIMER_LENGTH_PROP, String.valueOf(timerLength));
		
		String queueName = "test";
		String message = "This is a test";
		
		long time = System.currentTimeMillis();
		
		Clock clock = mock(Clock.class);
		when(clock.getTimeInNano()).thenReturn(time);
		
		FileQueueService service = new FileQueueService(TEST_DIR, clock);
		
		service.push(queueName, message);
		
		String message1 = service.pull(queueName);
		
		//Return the same time, which should make the message be invisible
		when(clock.getTimeInNano()).thenReturn(time);
		String message2 = service.pull(queueName);
		
		assertEquals(message1, message);
		assertTrue(message2.isEmpty());
	}
	
	@Test
	public void testPullAfterDeletion() throws QueueNotFoundException, InterruptedException, IOException {
		
		long timerLength = 30000;
		System.setProperty(FileQueueService.TIMER_LENGTH_PROP, String.valueOf(timerLength));
		
		String queueName = "test";
		String message = "This is a test";
		String message2 = "Second Test";
		
		long time = System.currentTimeMillis();
		
		Clock clock = mock(Clock.class);
		when(clock.getTimeInNano()).thenReturn(time);
		
		
		FileQueueService service = new FileQueueService(TEST_DIR, clock);
		
		service.push(queueName, message);
		service.push(queueName, message2);
		
		String firstMessage = service.pull(queueName);
		service.delete(queueName, message);
		
		//Just in case the visibility timeout would cause this test to fail
		when(clock.getTimeInNano()).thenReturn(time + (timerLength) * 2);
		String secondMessage = service.pull(queueName);
		
		assertEquals(firstMessage, message);
		assertEquals(secondMessage, message2);
	}
	
	@Test(expected = QueueNotFoundException.class)
	public void testQueueNotFound() throws QueueNotFoundException, IOException, InterruptedException {
		
		String queueName = "Not Found";
		Clock clock = new Clock();
		
		FileQueueService service = new FileQueueService(TEST_DIR, clock);
		
		service.pull(queueName);
	}
	
	@After
	public void tearDown() throws IOException {
		
		//Deleting test directory
		if (!TEST_DIR.mkdir()) {
			delete(TEST_DIR);
		}
	}
	
	private void delete(File f) throws IOException {
		
		if (f.isDirectory()) {
			for (File c : f.listFiles()) {
			  delete(c);
			}
		}

		if (!f.delete()) {
			throw new FileNotFoundException("Failed to delete file: " + f);
		}
	}
}
