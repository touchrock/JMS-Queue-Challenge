package com.example.queue.service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import com.example.queue.service.exception.QueueNotFoundException;

public class FileQueueService implements QueueService {
	
	//Used as a cache. All methods that touch this must be thread safe. 
	private List<FileMessageQueue> queues = new ArrayList<FileMessageQueue>();
	private Clock clock;
	
	private File messageDirectory;
	
	//Default timer to 30 seconds
	private long timerLength = 30 * (10^10);
	
	public final static String TIMER_LENGTH_PROP = "Timer.Length.Nano";
	
	public FileQueueService(File messageDirectory, Clock clock) {

		this.messageDirectory = messageDirectory;
		
		messageDirectory.mkdir();
		
		this.clock = clock;
		
		String timerLengthStr = System.getProperty(TIMER_LENGTH_PROP);
		
		if (timerLengthStr != null && !timerLengthStr.isEmpty()) {
			try {
				timerLength = Long.parseLong(timerLengthStr);
			}
			catch (NumberFormatException e) {
				//Move forward with default timer
				System.out.println("The System Property = " + TIMER_LENGTH_PROP + " should be a number, but was " + timerLengthStr);
			}
		}
	}
	
	//This lock method makes the push, pull, and delete methods threadsafe as well as cross jvm safe
	private void lock(File lock) throws InterruptedException {
		
		while (!lock.mkdir()) {
		  Thread.sleep(50);
		}
	}

	private void unlock(File lock) {
		lock.delete();
	}

	@Override
	public void push(String queueName, String message)
	  throws InterruptedException, IOException {
		
		FileMessageQueue messageQueue = null;
		
		try {
			messageQueue = getMessageQueue(queueName);
		}
		catch (QueueNotFoundException e) {
			messageQueue = createMessageQueue(queueName);
		}
		
		File messages = messageQueue.getMessageFile();
	
		lock(messageQueue.getLockFile());
		try (PrintWriter pw = new PrintWriter(new FileWriter(messages, true))) {
			
			pw.println(new Record(message).toString());
		}
		finally {
		  unlock(messageQueue.getLockFile());
		}
	}

	@Override
	public String pull(String queueName) throws QueueNotFoundException, IOException, InterruptedException {
		
		FileMessageQueue messageQueue = getMessageQueue(queueName);
		
		lock(messageQueue.getLockFile());
		try {
			List<String> messages = new ArrayList<>(Files.readAllLines(messageQueue.getMessageFile().toPath(), StandardCharsets.UTF_8));
			
			for (int i = 0; i < messages.size(); i++) {
				
				Record record = Record.parseRecord(messages.get(i));
				
				if (record.getTimePolledInNano() != 0l) {
					if (record.getTimePolledInNano() + timerLength < clock.getTimeInNano()) {
						
						record.setTimePolledInNano(clock.getTimeInNano());
						messages.set(i, record.toString());
						Files.write(messageQueue.getMessageFile().toPath(), messages, StandardCharsets.UTF_8);
						return record.getMessage();
					}
				}
				else {
				
					record.setTimePolledInNano(clock.getTimeInNano());
					messages.set(i, record.toString());
					Files.write(messageQueue.getMessageFile().toPath(), messages, StandardCharsets.UTF_8);
					return  record.getMessage();
				}	
			}
		} 
		finally {
			unlock(messageQueue.getLockFile());
		}
			
		return "";
	}

	@Override
	public void delete(String queueName, String message)
			throws QueueNotFoundException, IOException, InterruptedException {
		
		FileMessageQueue messageQueue = getMessageQueue(queueName);
		
		lock(messageQueue.getLockFile());
		try {
			List<String> messages = new ArrayList<>(Files.readAllLines(messageQueue.getMessageFile().toPath(), StandardCharsets.UTF_8));
			
			for (int i = 0; i < messages.size(); i++) {
				
				Record record = Record.parseRecord(messages.get(i));
				
				if (record.getMessage().equals(message)) {
					messages.remove(i);
					Files.write(messageQueue.getMessageFile().toPath(), messages, StandardCharsets.UTF_8);
					break;
				}
			}
		} 
		finally {
			unlock(messageQueue.getLockFile());
		}
	}
	
	private FileMessageQueue createMessageQueue(String queueName) {
		
		File queueDir = new File(messageDirectory.getAbsolutePath() + File.separator + queueName);
		queueDir.mkdir();
		
		return createMessageQueue(queueName, queueDir);
	}
	
	private synchronized FileMessageQueue createMessageQueue(String queueName, File queueDir) {
		
		FileMessageQueue queue = new FileMessageQueue(queueName);
		
		queue.setQueueDir(queueDir);
		
		File lockFile = new File(queueDir + File.separator + FileMessageQueue.LOCK_FILE);
		queue.setLockFile(lockFile);
		
		File messageFile = new File(queueDir + File.separator + FileMessageQueue.MESSAGE_FILE);
		queue.setMessageFile(messageFile);
		
		queues.add(queue);
		return queue;
	}
	
	/**
	 * Check the instance of the object for the queue, if not found then look on file system in case another jvm made the queue.
	 * @param queueName
	 * @return FileMessageQueue
	 * @throws QueueNotFoundException
	 */
	private synchronized FileMessageQueue getMessageQueue(String queueName) throws QueueNotFoundException {
		
		FileMessageQueue queue = new FileMessageQueue(queueName);
		
		//Check the cache for the queue to avoid unnecessary I/O.
		if (queues.contains(queue)) {
			return queues.get(queues.indexOf(queue));
		}
		
		for (File file : messageDirectory.listFiles()) {
			if (file.getName().equals(queueName)) {
				return createMessageQueue(queueName, file);
			}
		}
		
		throw new QueueNotFoundException("Queue named: " + queueName + " was not found.");
	}
}
