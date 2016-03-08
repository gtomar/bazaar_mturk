package basilica2.myagent.listeners;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.cmu.cs.lti.basilica2.core.Event;
import basilica2.agents.components.InputCoordinator;
import basilica2.agents.events.MessageEvent;
import basilica2.agents.events.PresenceEvent;
import basilica2.agents.events.PromptEvent;
import basilica2.agents.listeners.BasilicaPreProcessor;
import basilica2.agents.listeners.MessageAnnotator;
import basilica2.social.events.DormantGroupEvent;
import basilica2.social.events.DormantStudentEvent;
import basilica2.socketchat.WebsocketChatClient;
import basilica2.tutor.events.DoTutoringEvent;

import org.apache.commons.lang3.StringUtils;
import org.apache.xerces.parsers.DOMParser;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import basilica2.myagent.Topic;
import basilica2.myagent.User;
import edu.cmu.cs.lti.basilica2.core.Event;
import edu.cmu.cs.lti.project911.utils.log.Logger;
import edu.cmu.cs.lti.project911.utils.time.TimeoutReceiver;
import edu.cmu.cs.lti.project911.utils.time.Timer;

public class Register implements BasilicaPreProcessor, TimeoutReceiver
{
	
    public void startTimer()
    {
    	timer = new Timer(3, this);
    	timer.start();
    }
    
	
	public Register() 
    {
    	
    	topicList = new ArrayList<Topic>();
    	userList = new ArrayList<User>();
    	lastConsolidation = 0;
    	reasoning = false;
    	
    	perspective_map = new HashMap<Integer, String>();
    	perspective_map.put(0,"most economical");
    	perspective_map.put(1,"environmental friendliness and low startup cost");
    	perspective_map.put(2,"carbon neutrality and economical in the long run");
    	perspective_map.put(3,"environmental friendliness and reliability");

    	plan_map = new ArrayList<Map<String, Integer>>();
    	Map<String, Integer> temp1 = new HashMap<String, Integer>();
    	temp1.put("reasoning",0);
    	temp1.put("non_reasoning",0);
    	
    	Map<String, Integer> temp2 = new HashMap<String, Integer>();
    	temp2.put("reasoning",0);
    	temp2.put("non_reasoning",0);
    	
    	Map<String, Integer> temp3 = new HashMap<String, Integer>();
    	temp3.put("reasoning",0);
    	temp3.put("non_reasoning",0);
    	
    	Map<String, Integer> temp4 = new HashMap<String, Integer>();
    	temp4.put("reasoning",0);
    	temp4.put("non_reasoning",0);
    	
    	plan_map.add(temp1);
    	plan_map.add(temp2);
    	plan_map.add(temp3);
    	plan_map.add(temp4);
    	
    	planList = new ArrayList<String>();
    	planList.add("PLAN1");
    	planList.add("PLAN2");
    	planList.add("PLAN3");
    	planList.add("PLAN4");
    	
    	
		String dialogueConfigFile="dialogues/dialogues-example.xml";
    	loadconfiguration(dialogueConfigFile);
    	startTimer();
	}    
    
	public ArrayList<Topic> topicList;	
	public ArrayList<User> userList;
	public ArrayList<String> planList;	
	public int lastConsolidation;
    public boolean reasoning;
    public String reasoning_from;
    public String reasoning_type;
    public static Timer timer;
    public InputCoordinator src;
    public Map<Integer, String> perspective_map;
    public ArrayList<Map<String, Integer>> plan_map;
	private void loadconfiguration(String f)
	{
		try
		{
			DOMParser parser = new DOMParser();
			parser.parse(f);
			Document dom = parser.getDocument();
			NodeList dialogsNodes = dom.getElementsByTagName("dialogs");
			if ((dialogsNodes != null) && (dialogsNodes.getLength() != 0))
			{
				Element conceptNode = (Element) dialogsNodes.item(0);
				NodeList conceptNodes = conceptNode.getElementsByTagName("dialog");
				if ((conceptNodes != null) && (conceptNodes.getLength() != 0))
				{
					for (int i = 0; i < conceptNodes.getLength(); i++)
					{
						Element conceptElement = (Element) conceptNodes.item(i);
						String conceptName = conceptElement.getAttribute("concept");
						String conceptDetailedName = conceptElement.getAttribute("description"); 
						Topic topic = new Topic(conceptName, conceptDetailedName);
						topicList.add(topic);
					}
				}
			}
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}	    
	}
	
	

	public Topic IsInTopicList(String concept)
	{
		for (int i = 0; i < topicList.size(); i++)
		{
			if (topicList.get(i).name.equals(concept))
			{
				return topicList.get(i);
			}
		}
		
		return null;
	}
	
	public int IsInUserList(String id)
	{
		for (int i = 0; i < userList.size(); i++)
		{
			//change it to id later
			if (userList.get(i).id.equals(id))
			{
				return i;
			}
		}
		
		return -1;
	}
	
	public void incrementScore(int increment)
	{
		for (int i = 0; i < userList.size(); i++)
		{
			userList.get(i).score += increment;
		}
	}
	/**
	 * @param source the InputCoordinator - to push new events to. (Modified events don't need to be re-pushed).
	 * @param event an incoming event which matches one of this preprocessor's advertised classes (see getPreprocessorEventClasses)
	 * 
	 * Preprocess an incoming event, by modifying this event or creating a new event in response. 
	 * All original and new events will be passed by the InputCoordinator to the second-stage Reactors ("BasilicaListener" instances).
	 */
	@Override
	public void preProcessEvent(InputCoordinator source, Event event)
	{
		src = source;
		if (event instanceof MessageEvent)
		{
			MessageEvent me = (MessageEvent)event;
			String[] annotations = me.getAllAnnotations();
			
			User user = getUser(me.getFrom());
			if(user == null) return;
			
			if(me.hasAnnotations("pos"))
			{

				if(userList.size() > 1)
				{
					
					int plan = 0;
					int count = 0;
						
					if (me.hasAnnotations("PLAN1"))
					{
						plan = 1;
						count++;
					}
					if (me.hasAnnotations("PLAN2"))
					{
						plan = 2;
						count++;
					}
					if (me.hasAnnotations("PLAN3"))
					{
						plan = 3;
						count++;
					}						
					if (me.hasAnnotations("PLAN4"))
					{
						plan = 4;
						count++;
					}
					
					if (plan == 0 && user.reasoning)
					{
						if (user.reasoning_type.contains("PLAN1"))
						{
							plan = 1;
							count++;
						}
						if (user.reasoning_type.contains("PLAN2"))
						{
							plan = 2;
							count++;
						}
						if (user.reasoning_type.contains("PLAN3"))
						{
							plan = 3;
							count++;
						}						
						if (user.reasoning_type.contains("PLAN4"))
						{
							plan = 4;
							count++;
						}
					}
					
					if (plan!=0  && !me.hasAnnotations("NEGATIVE"))
					{
						int temporary = plan_map.get(plan-1).get("reasoning") + 1;
						plan_map.get(plan-1).put("reasoning", temporary);
						
						User selected_user = choose_random_user(me.getFrom(), plan);
						if(selected_user != null)
						{
							if(count > 1)
							{
								String prompt_message_ = "Hey " + selected_user.name + ", Can you evaluate " + me.getFrom() + "'s choice from your perspective of " + 
										perspective_map.get(selected_user.perspective) + " ?";
								PromptEvent prompt = new PromptEvent(source,prompt_message_,"plan_reasoning");
								source.queueNewEvent(prompt);								
							}
							else{
								String prompt_message_ = "Hey " + selected_user.name + ", Can you evaluate " + me.getFrom() + "'s plan from your perspective of " + 
										perspective_map.get(selected_user.perspective) + " ?";
								PromptEvent prompt = new PromptEvent(source,prompt_message_,"plan_reasoning");
								source.queueNewEvent(prompt);

								selected_user.reasoning_flag[plan-1] = true;
							}
						}
					}
				}
				
				
					
				if(user.reasoning) 
				{
					user.reasoning = false;
					user.wait_duration = 0;
				}
			}
			else if(me.hasAnnotations("PLAN"))
			{
				
				if(user.reasoning)
				{
					if (planList.contains(user.reasoning_type))
					{

						int plan = 0;
						int count = 0;
						
						if (me.hasAnnotations("PLAN1"))
						{
							plan = 1;
							count++;
						}
						if (me.hasAnnotations("PLAN2"))
						{
							plan = 2;
							count++;
						}
						if (me.hasAnnotations("PLAN3"))
						{
							plan = 3;
							count++;
						}						
						if (me.hasAnnotations("PLAN4"))
						{
							plan = 4;
							count++;
						}
						
						if (plan == 0 && user.reasoning)
						{
							if (user.reasoning_type.contains("PLAN1"))
							{
								plan = 1;
								count++;
							}
							if (user.reasoning_type.contains("PLAN2"))
							{
								plan = 2;
								count++;
							}
							if (user.reasoning_type.contains("PLAN3"))
							{
								plan = 3;
								count++;
							}						
							if (user.reasoning_type.contains("PLAN4"))
							{
								plan = 4;
								count++;
							}
						}
						
						if (plan!=0 && !user.reasoning_flag[plan-1])
						{
							int temporary = plan_map.get(plan-1).get("non_reasoning") + 1;
							plan_map.get(plan-1).put("non_reasoning", temporary);
							if(count > 1)
							{
								String prompt_message_ = "Hey " + me.getFrom() + ", Can you elaborate on your choice from your perspective of " + 
										perspective_map.get(user.perspective) + " ?";
								PromptEvent prompt = new PromptEvent(source,prompt_message_,"plan_reasoning");
								source.queueNewEvent(prompt);	
							}
							else
							{
								String prompt_message_ = "Hey " + me.getFrom() + ", Can you evaluate plan " + Integer.toString(plan) + " from your perspective of " + 
									perspective_map.get(user.perspective) + " ?";
								PromptEvent prompt = new PromptEvent(source,prompt_message_,"plan_reasoning");
								source.queueNewEvent(prompt);
								
								user.reasoning_flag[plan-1] = true;
							}
						}

					}

					user.reasoning = false;
					user.wait_duration = 0;
				}
				else
				{
					user.reasoning = true;

					user.reasoning_type = "";
					if (me.hasAnnotations("PLAN1"))
					{
						user.reasoning_type += "PLAN1";
					}
					if (me.hasAnnotations("PLAN2"))
					{
						user.reasoning_type += "PLAN2";
					}
					if (me.hasAnnotations("PLAN3"))
					{
						user.reasoning_type += "PLAN3";
					}						
					if (me.hasAnnotations("PLAN4"))
					{
						user.reasoning_type += "PLAN4";
					}
					
					user.wait_duration = 10;
				}

			}
			else if(user.reasoning)
			{
				if (planList.contains(user.reasoning_type))
				{
					int plan = 0;
					int count = 0;
					
					if (me.hasAnnotations("PLAN1"))
					{
						plan = 1;
						count++;
					}
					if (me.hasAnnotations("PLAN2"))
					{
						plan = 2;
						count++;
					}
					if (me.hasAnnotations("PLAN3"))
					{
						plan = 3;
						count++;
					}						
					if (me.hasAnnotations("PLAN4"))
					{
						plan = 4;
						count++;
					}
					
					if (plan == 0 && user.reasoning)
					{
						if (user.reasoning_type.contains("PLAN1"))
						{
							plan = 1;
							count++;
						}
						if (user.reasoning_type.contains("PLAN2"))
						{
							plan = 2;
							count++;
						}
						if (user.reasoning_type.contains("PLAN3"))
						{
							plan = 3;
							count++;
						}						
						if (user.reasoning_type.contains("PLAN4"))
						{
							plan = 4;
							count++;
						}
					}
					
					if (plan!=0 && !user.reasoning_flag[plan-1])
					{
						int temporary = plan_map.get(plan-1).get("non_reasoning") + 1;
						plan_map.get(plan-1).put("non_reasoning", temporary);
						if(count > 1)
						{
							String prompt_message_ = "Hey " + me.getFrom() + ", Can you elaborate on choice from your perspective of " + 
									perspective_map.get(user.perspective) + " ?";
							PromptEvent prompt = new PromptEvent(source,prompt_message_,"plan_reasoning");
							source.queueNewEvent(prompt);
						}
						else
						{
							String prompt_message_ = "Hey " + me.getFrom() + ", Can you evaluate plan " + Integer.toString(plan) + " from your perspective of " + 
								perspective_map.get(user.perspective) + " ?";
							PromptEvent prompt = new PromptEvent(source,prompt_message_,"plan_reasoning");
							source.queueNewEvent(prompt);
							
							user.reasoning_flag[plan-1] = true;
						}
					}
	
				}

				user.reasoning = false;
				user.wait_duration = 0;
			}
			
			
	    }
		else if (event instanceof DormantGroupEvent)
		{
		
			String prompt_message = "It looks like no one is using the chat. Use this space to discuss and come to a consensus about which plan you prefer while writing the proposal.";
			PromptEvent prompt = new PromptEvent(source, prompt_message , "POKING");
			source.queueNewEvent(prompt);
					
		}
		else if (event instanceof PresenceEvent)
		{
			PresenceEvent pe = (PresenceEvent) event;

			if (!pe.getUsername().contains("Agent") && !source.isAgentName(pe.getUsername()))
			{

				String username = pe.getUsername();
				String userid = pe.getUserId();

				int userperspective = Integer.parseInt(pe.getUserPerspective());
				
				if(userid == null)
					return;
				Date date= new Date();
				Timestamp currentTimestamp= new Timestamp(date.getTime());
				int userIndex = IsInUserList(userid);
				if (pe.getType().equals(PresenceEvent.PRESENT))
				{
					System.out.println("Someone present");
					if(userIndex == -1)
					{
						String prompt_message = "Welcome, " + username + "\n";
						
						User newuser = new User(username, userid, currentTimestamp, userperspective);
						userList.add(newuser);
						System.out.println("Someone joined with id = " + userid);
						
						PromptEvent prompt = new PromptEvent(source, prompt_message , "INTRODUCTION");
						source.queueNewEvent(prompt);
					}
					
				}
				else if (pe.getType().equals(PresenceEvent.ABSENT))
				{
					System.out.println("Someone left");
					if(userIndex != -1)
					{
					    System.out.println("Someone left with id = " + userid);
						userList.remove(userIndex);
	     				checkOutdatedTopics();
					}
				}
			}
		}
	}
	
    public String discussedTopics()
    {
    	ArrayList<String> discussed_topics = new ArrayList<String>();
    	for (int i = 0; i < topicList.size(); i++)
		{
			Topic topic = topicList.get(i);
			if (topic.topic_detected  != null ||
			    topic.topic_discussed != null || 
			    topic.topic_requested != null 
				)
				{
					discussed_topics.add(topic.detailed_name);
				}
		}
    	
    	return  discussed_topics.size() > 0 ? StringUtils.join(discussed_topics) : null;
    }
	public void checkOutdatedTopics()
	{
		Timestamp oldestStudent = oldestStudent();
		
		if(oldestStudent == null)
		{
			for (int i = 0; i < topicList.size(); i++)
			{
				topicList.get(i).topic_detected = null;
				topicList.get(i).topic_discussed = null;
				topicList.get(i).topic_prompted = null;
				topicList.get(i).topic_requested = null;
			}
		}
		else
		{
			for (int i = 0; i < topicList.size(); i++)
			{
				Topic topic = topicList.get(i);
				if ((topic.topic_detected  == null || topic.topic_detected.before(oldestStudent))  &&
					(topic.topic_discussed == null || topic.topic_discussed.before(oldestStudent)) &&
					(topic.topic_prompted  == null || topic.topic_prompted.before(oldestStudent))  &&
					(topic.topic_requested == null || topic.topic_requested.before(oldestStudent))
				   )
				{
					topicList.get(i).topic_detected = null;
					topicList.get(i).topic_discussed = null;
					topicList.get(i).topic_prompted = null;
					topicList.get(i).topic_requested = null;					
				}
			}
		}
	}
	
	public User getUser(String name)
	{
		for (int i = 0; i < userList.size(); i++)
		{
			System.out.println(userList.get(i).name);;
			if (userList.get(i).name.equals(name))
			{
				return userList.get(i);
			}
		}
		
		return null;
		
	}
	
	public User choose_random_user(String name, int plan )
	{
		int index = (int) (Math.random() * (userList.size() - .1));
		boolean found = false;
		int tries = 0;
		while((!found) && tries < 10)
		{
			index = (int) (Math.random() * (userList.size() - .1));
			User user = userList.get(index);
			if(!user.reasoning_flag[plan-1] && !userList.get(index).name.equals(name))
			{
				found = true;
				return userList.get(index);
			}
			tries++;
		}
		return null;
	}
	
	public Timestamp oldestStudent()
	{
		Timestamp minTimestamp = null;
		
		for (int i = 0; i < userList.size(); i++)
		{
			if (minTimestamp == null || userList.get(i).time_of_entry.before(minTimestamp))
			{
				minTimestamp = userList.get(i).time_of_entry;
			}
		}
		
		return minTimestamp;
	}
	
	public String oldestStudentName()
	{
		Timestamp minTimestamp = null;
		String name = "";
		
		for (int i = 0; i < userList.size(); i++)
		{
			if (minTimestamp == null || userList.get(i).time_of_entry.before(minTimestamp))
			{
				minTimestamp = userList.get(i).time_of_entry;
				name = userList.get(i).name;
			}
		}
		
		return name;
	}
	
	/**
	 * @return the classes of events that this Preprocessor cares about
	 */
	@Override
	public Class[] getPreprocessorEventClasses()
	{
		//only MessageEvents will be delivered to this watcher.
		return new Class[]{MessageEvent.class, DormantGroupEvent.class, PresenceEvent.class, DormantStudentEvent.class};
	}


	@Override
	public void log(String arg0, String arg1, String arg2) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void timedOut(String arg0) {
		// TODO Auto-generated method stub
		
		for (int i =0 ;i< plan_map.size(); i++)
		{
		    Iterator it = ( plan_map.get(i)).entrySet().iterator();
		    while (it.hasNext()) {
		        Map.Entry pair = (Map.Entry)it.next();
		        //System.out.println(pair.getKey() + " = " + pair.getValue());
		        //it.remove(); // avoids a ConcurrentModificationException
		    }
		}
		for (int i = 0; i < userList.size(); i++)
		{
			if (userList.get(i).wait_duration == 0)
			{
				User user = userList.get(i);
				if(user.reasoning)
				{
					if (1==1)
					{
						int plan = 0;
						int count = 0;
						if (plan == 0 && user.reasoning)
						{
							if (user.reasoning_type.contains("PLAN1"))
							{
								plan = 1;
								count++;
							}
							if (user.reasoning_type.contains("PLAN2"))
							{
								plan = 2;
								count++;
							}
							if (user.reasoning_type.contains("PLAN3"))
							{
								plan = 3;
								count++;
							}						
							if (user.reasoning_type.contains("PLAN4"))
							{
								plan = 4;
								count++;
							}
						}
						
						if (plan!=0  && !user.reasoning_flag[plan-1])
						{
							int temporary = plan_map.get(plan-1).get("non_reasoning") + 1;
							plan_map.get(plan-1).put("non_reasoning", temporary);
		
							if(count > 1)
							{
								String prompt_message_ = "Hey " + user.name + ", Can you elaborate on your choice from your perspective of " + 
										perspective_map.get(user.perspective) + " ?";
								PromptEvent prompt = new PromptEvent(src,prompt_message_,"plan_reasoning");
								src.queueNewEvent(prompt);
							}
							else
							{
								String prompt_message_ = "Hey " + user.name + ", Can you evaluate plan " + Integer.toString(plan) + " from your perspective of " + 
									perspective_map.get(user.perspective) + " ?";
								PromptEvent prompt = new PromptEvent(src,prompt_message_,"plan_reasoning");
								src.queueNewEvent(prompt);
								
								user.reasoning_flag[plan-1] = true;
							}
						}
						

					}

					user.reasoning = false;
					user.wait_duration = 0;
				}
			}
			else
			{
				userList.get(i).wait_duration = userList.get(i).wait_duration - 5;
			}
		}
		
		startTimer();
		
	}
}
