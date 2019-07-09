package com.example.queue.service;

import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.Test;

import com.example.queue.service.Clock;
import com.example.queue.service.FileQueueService;
import com.example.queue.service.InMemoryQueueService;
import com.example.queue.service.exception.QueueNotFoundException;

import static org.mockito.Mockito.*;

public class InMemoryQueueTest {

	@Test
	public void testPushAndPull() throws QueueNotFoundException {
		
		System.setProperty(InMemoryQueueService.TIMER_LENGTH_PROP, "30000");
		
		String queueName = "test";
		String message = "This is a test";
		
		InMemoryQueueService service = new InMemoryQueueService(new Clock());
		
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
		
		InMemoryQueueService service = new InMemoryQueueService(new Clock());
		
		service.push(queue1, message1);
		service.push(queue2, message2);
		
		assertEquals(message1, service.pull(queue1));
		assertEquals(message2, service.pull(queue2));
	}
	
	@Test
	public void testPullLessThanTimer() throws QueueNotFoundException, InterruptedException, IOException {
		
		long timerLength = 30000;
		System.setProperty(InMemoryQueueService.TIMER_LENGTH_PROP, String.valueOf(timerLength));
		
		String queueName = "test";
		String message = "This is a test";
		
		long time = System.currentTimeMillis();
		
		Clock clock = mock(Clock.class);
		when(clock.getTimeInNano()).thenReturn(time);
		
		InMemoryQueueService service = new InMemoryQueueService(clock);
		
		service.push(queueName, message);
		
		String message1 = service.pull(queueName);
		
		//Return the same time, which should make the message be invisible
		when(clock.getTimeInNano()).thenReturn(time);
		String message2 = service.pull(queueName);
		
		assertEquals(message1, message);
		assertTrue(message2.isEmpty());
	}
	
	/**
	 * The test will check the reliability behavior of the queue service.
	 *  The message will reach its visibility limit then the message will become visible again after the first pull.
	 *  Thus, a second pull will return the first message.
	 * @throws QueueNotFoundException
	 * @throws InterruptedException 
	 */
	@Test
	public void testPullMoreThanTimer() throws QueueNotFoundException, InterruptedException {
		
		long timerLength = 30000;
		System.setProperty(InMemoryQueueService.TIMER_LENGTH_PROP, String.valueOf(timerLength));
		
		String queueName = "test";
		String message = "This is a test";
		
		Clock clock = mock(Clock.class);
		when(clock.getTimeInNano()).thenReturn(System.currentTimeMillis());
		
		InMemoryQueueService service = new InMemoryQueueService(clock);
		
		service.push(queueName, message);
		
		String message1 = service.pull(queueName);
		
		//Return double the timer length when clock time is called. Causing message to become visible again
		when(clock.getTimeInNano()).thenReturn(System.currentTimeMillis() + (timerLength * 2));
		String message2 = service.pull(queueName);
		
		assertEquals(message1, message2);
	}
	
	@Test
	public void testPullAfterDeletion() throws QueueNotFoundException, InterruptedException {
		
		long timerLength = 30000;
		System.setProperty(InMemoryQueueService.TIMER_LENGTH_PROP, String.valueOf(timerLength));
		
		String queueName = "test";
		String message = "This is a test";
		String message2 = "Second Test";
		
		long time = System.currentTimeMillis();
		
		Clock clock = mock(Clock.class);
		when(clock.getTimeInNano()).thenReturn(time);
		
		InMemoryQueueService service = new InMemoryQueueService(clock);
		
		service.push(queueName, message);
		service.push(queueName, message2);
		
		String firstMessage = service.pull(queueName);
		
		//Just in case the visibility timeout would cause this test to fail
		when(clock.getTimeInNano()).thenReturn(time + (timerLength) * 2);
		service.delete(queueName, message);
		
		String secondMessage = service.pull(queueName);
		
		assertEquals(firstMessage, message);
		assertEquals(secondMessage, message2);
	}
	
	@Test(expected = QueueNotFoundException.class)
	public void testQueueNotFound() throws QueueNotFoundException {
		
		String queueName = "Not Found";
		Clock clock = new Clock();
		
		InMemoryQueueService service = new InMemoryQueueService(clock);
		
		service.pull(queueName);
	}
}
