package com.example.queue.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.CreateQueueRequest;
import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.GetQueueUrlRequest;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.QueueDoesNotExistException;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.SendMessageRequest;

public class SqsQueueService implements QueueService {
	
	//Cache for pulled messages. All methods that touch this must be thread safe
	private List<Message> pulledMessages = new ArrayList<Message>();
	
	private AmazonSQSClient sqsClient;
	
  public SqsQueueService(AmazonSQSClient sqsClient) {
	  this.sqsClient = sqsClient;
  }

	@Override
	public void push(String queueName, String message) throws InterruptedException,
			IOException {
		
		String queueUrl = null;
		
		try {
			queueUrl = getQueueUrl(queueName);
		}
		catch (QueueDoesNotExistException e) {
			CreateQueueRequest createQueueRequest = new CreateQueueRequest().withQueueName(queueName);
			queueUrl = sqsClient.createQueue(createQueueRequest).getQueueUrl();
		}
		
		sqsClient.sendMessage(new SendMessageRequest()
								.withQueueUrl(queueUrl)
								.withMessageBody(message));
	}
	
	@Override
	public synchronized String pull(String queueName) throws IOException, InterruptedException {
		
		String queueUrl = getQueueUrl(queueName);
		
		ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest(queueUrl);
		List<Message> messages = sqsClient.receiveMessage(receiveMessageRequest).getMessages();
		
		for (Message message : messages) {
			
			pulledMessages.add(message);
			return message.getBody();
		}
		
		return "";
	}
	
	/**
	 * Delete a message that was pulled earlier.
	 */
	@Override
	public synchronized void delete(String queueName, String message) throws IOException, InterruptedException {

		String queueUrl = getQueueUrl(queueName);
		
		for (int i = 0; i < pulledMessages.size(); i++) {
			
			Message pulledMessage = pulledMessages.get(i);
			
			if (pulledMessage.getBody().equals(message)) {
				
				String messageReceiptHandle = pulledMessage.getReceiptHandle();
				
				sqsClient.deleteMessage(new DeleteMessageRequest()
								    .withQueueUrl(queueUrl)
								    .withReceiptHandle(messageReceiptHandle));
				
				pulledMessages.remove(i);
				
				break;
			}
		}
	}
	
	private String getQueueUrl(String queueName) throws QueueDoesNotExistException {
		
		return sqsClient.getQueueUrl(new GetQueueUrlRequest().withQueueName(queueName)).getQueueUrl();
	}
}
