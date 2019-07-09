package com.example.queue.service;

import java.io.IOException;

import com.example.queue.service.exception.QueueNotFoundException;

public interface QueueService {

	/**
	 * Look for the queue, if not found create a new queue based on the queue name parameter. 
	 * Place message on queue once it is found or created.
	 * @param queueName
	 * @param message
	 * @throws InterruptedException
	 * @throws IOException
	 */
	public void push(String queueName, String message) throws InterruptedException, IOException;
	
	/**
	 * The first visible message is returned, or an empty string is returned if there are no messages.
	 * @param queueName
	 * @return String
	 * @throws QueueNotFoundException
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public String pull(String queueName) throws QueueNotFoundException, IOException, InterruptedException;
	
	
	/**
	 * Remove message from queue.
	 * @param queueName
	 * @param message
	 * @throws QueueNotFoundException
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public void delete(String queueName, String message) throws QueueNotFoundException, IOException, InterruptedException;

}
