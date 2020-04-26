package org.sodeac.common.message.dispatcher.setup;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.sodeac.common.message.dispatcher.setup.MessageConsumerFeature;
import org.sodeac.common.message.dispatcher.setup.MessageConsumerFeature.ConsumerRule;
import org.sodeac.common.message.dispatcher.setup.MessageConsumerFeature.ConsumerRule.TriggerByMessageAgeMode;
import org.sodeac.common.message.dispatcher.setup.MessageDispatcherChannelSetup.IChannelFeature;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class MessageConsumerFeatureBuilderTest
{
	@Test
	public void test00001_BuilderSynthaxPhase1()
	{
		
		IChannelFeature feature = MessageConsumerFeature.newBuilder().consumeMessage((m,h) -> m.getPayload().split("\\."),String.class).immediately().buildFeature();
		
		assertNotNull("object should not be  null",feature);
		List<ConsumerRule> ruleList = ((MessageConsumerFeature.MessageConsumerFeatureConfiguration)feature).getConsumerRuleList();
		assertEquals("value should be correct", 1, ruleList.size());
		ConsumerRule consumerRule = ruleList.get(0);
		assertNull("value should be correct", consumerRule.getPoolFilter());
		assertEquals("value should be correct",1, consumerRule.getPoolMinSize());
		assertEquals("value should be correct",Short.MAX_VALUE, consumerRule.getPoolMaxSize());
		assertNotNull("value should be correct", consumerRule.getMessageConsumer());
		
		
		feature = MessageConsumerFeature.newBuilder().inMessageMonitoringPool().useFilter(m -> true).consumeMessage((m,h) -> m.getChannel()).immediately().buildFeature();
		
		assertNotNull("object should not be  null",feature);
		ruleList = ((MessageConsumerFeature.MessageConsumerFeatureConfiguration)feature).getConsumerRuleList();
		assertEquals("value should be correct", 1, ruleList.size());
		consumerRule = ruleList.get(0);
		assertNotNull("value should be correct", consumerRule.getPoolFilter());
		assertEquals("value should be correct",1, consumerRule.getPoolMinSize());
		assertEquals("value should be correct",Short.MAX_VALUE, consumerRule.getPoolMaxSize());
		assertNotNull("value should be correct", consumerRule.getMessageConsumer());
		
		feature = MessageConsumerFeature.newBuilder().inMessageMonitoringPool().minPoolSize(13).consumeMessage((m,h) -> m.getChannel()).immediately().buildFeature();
		
		assertNotNull("object should not be  null",feature);
		ruleList = ((MessageConsumerFeature.MessageConsumerFeatureConfiguration)feature).getConsumerRuleList();
		assertEquals("value should be correct", 1, ruleList.size());
		consumerRule = ruleList.get(0);
		assertNull("value should be correct", consumerRule.getPoolFilter());
		assertEquals("value should be correct",13, consumerRule.getPoolMinSize());
		assertEquals("value should be correct",Short.MAX_VALUE, consumerRule.getPoolMaxSize());
		assertNotNull("value should be correct", consumerRule.getMessageConsumer());
		
		feature = MessageConsumerFeature.newBuilder().inMessageMonitoringPool().maxPoolSize(13).consumeMessage((m,h) -> m.getChannel()).immediately().buildFeature();
		
		assertNotNull("object should not be  null",feature);
		ruleList = ((MessageConsumerFeature.MessageConsumerFeatureConfiguration)feature).getConsumerRuleList();
		assertEquals("value should be correct", 1, ruleList.size());
		consumerRule = ruleList.get(0);
		assertNull("value should be correct", consumerRule.getPoolFilter());
		assertEquals("value should be correct",1, consumerRule.getPoolMinSize());
		assertEquals("value should be correct",13, consumerRule.getPoolMaxSize());
		assertNotNull("value should be correct", consumerRule.getMessageConsumer());
		
		feature = MessageConsumerFeature.newBuilder().inMessageMonitoringPool().minPoolSize(7).maxPoolSize(13).consumeMessage((m,h) -> m.getChannel()).immediately().buildFeature();
		
		assertNotNull("object should not be  null",feature);
		ruleList = ((MessageConsumerFeature.MessageConsumerFeatureConfiguration)feature).getConsumerRuleList();
		assertEquals("value should be correct", 1, ruleList.size());
		consumerRule = ruleList.get(0);
		assertNull("value should be correct", consumerRule.getPoolFilter());
		assertEquals("value should be correct",7, consumerRule.getPoolMinSize());
		assertEquals("value should be correct",13, consumerRule.getPoolMaxSize());
		assertNotNull("value should be correct", consumerRule.getMessageConsumer());
		
		feature = MessageConsumerFeature.newBuilder().inMessageMonitoringPool().useFilter(m -> true).minPoolSize(13).consumeMessage((m,h) -> m.getChannel()).immediately().buildFeature();
		
		assertNotNull("object should not be  null",feature);
		ruleList = ((MessageConsumerFeature.MessageConsumerFeatureConfiguration)feature).getConsumerRuleList();
		assertEquals("value should be correct", 1, ruleList.size());
		consumerRule = ruleList.get(0);
		assertNotNull("value should be correct", consumerRule.getPoolFilter());
		assertEquals("value should be correct",13, consumerRule.getPoolMinSize());
		assertEquals("value should be correct",Short.MAX_VALUE, consumerRule.getPoolMaxSize());
		assertNotNull("value should be correct", consumerRule.getMessageConsumer());
		
		feature = MessageConsumerFeature.newBuilder().inMessageMonitoringPool().useFilter(m -> true).maxPoolSize(13).consumeMessage((m,h) -> m.getChannel()).immediately().buildFeature();
		
		assertNotNull("object should not be  null",feature);
		ruleList = ((MessageConsumerFeature.MessageConsumerFeatureConfiguration)feature).getConsumerRuleList();
		assertEquals("value should be correct", 1, ruleList.size());
		consumerRule = ruleList.get(0);
		assertNotNull("value should be correct", consumerRule.getPoolFilter());
		assertEquals("value should be correct",1, consumerRule.getPoolMinSize());
		assertEquals("value should be correct",13, consumerRule.getPoolMaxSize());
		assertNotNull("value should be correct", consumerRule.getMessageConsumer());
		
		feature = MessageConsumerFeature.newBuilder().inMessageMonitoringPool().useFilter(m -> true).minPoolSize(7).maxPoolSize(13).consumeMessage((m,h) -> m.getChannel()).immediately().buildFeature();
		
		assertNotNull("object should not be  null",feature);
		ruleList = ((MessageConsumerFeature.MessageConsumerFeatureConfiguration)feature).getConsumerRuleList();
		assertEquals("value should be correct", 1, ruleList.size());
		consumerRule = ruleList.get(0);
		assertNotNull("value should be correct", consumerRule.getPoolFilter());
		assertEquals("value should be correct",7, consumerRule.getPoolMinSize());
		assertEquals("value should be correct",13, consumerRule.getPoolMaxSize());
		assertNotNull("value should be correct", consumerRule.getMessageConsumer());
		
	}
	
	@Test
	public void test00002_BuilderSynthaxPhase2()
	{
		IChannelFeature feature = MessageConsumerFeature.newBuilder().consumeMessage((m,h) -> m.getChannel()).immediately().buildFeature();
		
		assertNotNull("object should not be  null",feature);
		List<ConsumerRule> ruleList = ((MessageConsumerFeature.MessageConsumerFeatureConfiguration)feature).getConsumerRuleList();
		assertEquals("value should be correct", 1, ruleList.size());
		ConsumerRule consumerRule = ruleList.get(0);
		assertEquals("value should be correct",-1, consumerRule.getMessageAgeTriggerAge());
		assertNotNull("value should be correct", consumerRule.getMessageConsumer());
		assertEquals("value should be correct",2, consumerRule.getGroupMembers().size());
		assertEquals("value should be correct",TriggerByMessageAgeMode.NONE, consumerRule.getMessageAgeTriggerMode());
		assertEquals("value should be correct",-1, consumerRule.getMessageAgeTriggerCount());
		assertEquals("value should be correct",-1, consumerRule.getMessageAgeTriggerAge());
		assertEquals("value should be correct",TimeUnit.SECONDS, consumerRule.getMessageAgeTriggerUnit());
		assertNull("value should be correct", consumerRule.getConsumeEventAgeTriggerGroup());
		assertEquals("value should be correct",-1, consumerRule.getConsumeEventAgeTriggerAge());
		assertEquals("value should be correct",TimeUnit.SECONDS, consumerRule.getConsumeEventAgeTriggerUnit());
		
		feature = MessageConsumerFeature.newBuilder().consumeMessage((m,h) -> m.getChannel()).memberOfGroup("ABC").immediately().buildFeature();
		
		assertNotNull("object should not be  null",feature);
		ruleList = ((MessageConsumerFeature.MessageConsumerFeatureConfiguration)feature).getConsumerRuleList();
		assertEquals("value should be correct", 1, ruleList.size());
		consumerRule = ruleList.get(0);
		assertEquals("value should be correct",-1, consumerRule.getMessageAgeTriggerAge());
		assertNotNull("value should be correct", consumerRule.getMessageConsumer());
		assertEquals("value should be correct",3, consumerRule.getGroupMembers().size());
		assertEquals("value should be correct",TriggerByMessageAgeMode.NONE, consumerRule.getMessageAgeTriggerMode());
		assertEquals("value should be correct",-1, consumerRule.getMessageAgeTriggerCount());
		assertEquals("value should be correct",-1, consumerRule.getMessageAgeTriggerAge());
		assertEquals("value should be correct",TimeUnit.SECONDS, consumerRule.getMessageAgeTriggerUnit());
		assertNull("value should be correct", consumerRule.getConsumeEventAgeTriggerGroup());
		assertEquals("value should be correct",-1, consumerRule.getConsumeEventAgeTriggerAge());
		assertEquals("value should be correct",TimeUnit.SECONDS, consumerRule.getConsumeEventAgeTriggerUnit());
		
		feature = MessageConsumerFeature.newBuilder().consumeMessage((m,h) -> m.getChannel()).memberOfGroup("ABC").andMemberOfGroup("123").immediately().buildFeature();
		
		assertNotNull("object should not be  null",feature);
		ruleList = ((MessageConsumerFeature.MessageConsumerFeatureConfiguration)feature).getConsumerRuleList();
		assertEquals("value should be correct", 1, ruleList.size());
		consumerRule = ruleList.get(0);
		assertEquals("value should be correct",-1, consumerRule.getMessageAgeTriggerAge());
		assertNotNull("value should be correct", consumerRule.getMessageConsumer());
		assertEquals("value should be correct",4, consumerRule.getGroupMembers().size());
		assertEquals("value should be correct",TriggerByMessageAgeMode.NONE, consumerRule.getMessageAgeTriggerMode());
		assertEquals("value should be correct",-1, consumerRule.getMessageAgeTriggerCount());
		assertEquals("value should be correct",-1, consumerRule.getMessageAgeTriggerAge());
		assertEquals("value should be correct",TimeUnit.SECONDS, consumerRule.getMessageAgeTriggerUnit());
		assertNull("value should be correct", consumerRule.getConsumeEventAgeTriggerGroup());
		assertEquals("value should be correct",-1, consumerRule.getConsumeEventAgeTriggerAge());
		assertEquals("value should be correct",TimeUnit.SECONDS, consumerRule.getConsumeEventAgeTriggerUnit());
		
	}
	
	@Test
	public void test00003_BuilderSynthaxPhase3()
	{
		IChannelFeature feature = MessageConsumerFeature.newBuilder().consumeMessage((m,h) -> m.getChannel()).ifAllMessagesAreOlderThan(13).seconds().buildFeature();
		
		assertNotNull("object should not be  null",feature);
		List<ConsumerRule> ruleList = ((MessageConsumerFeature.MessageConsumerFeatureConfiguration)feature).getConsumerRuleList();
		assertEquals("value should be correct", 1, ruleList.size());
		ConsumerRule consumerRule = ruleList.get(0);
		assertNotNull("value should be correct", consumerRule.getMessageConsumer());
		assertEquals("value should be correct",TriggerByMessageAgeMode.ALL, consumerRule.getMessageAgeTriggerMode());
		assertEquals("value should be correct",-1, consumerRule.getMessageAgeTriggerCount());
		assertEquals("value should be correct",13, consumerRule.getMessageAgeTriggerAge());
		assertEquals("value should be correct",TimeUnit.SECONDS, consumerRule.getMessageAgeTriggerUnit());
		assertNull("value should be correct", consumerRule.getConsumeEventAgeTriggerGroup());
		assertEquals("value should be correct",-1, consumerRule.getConsumeEventAgeTriggerAge());
		assertEquals("value should be correct",TimeUnit.SECONDS, consumerRule.getConsumeEventAgeTriggerUnit());
		
		feature = MessageConsumerFeature.newBuilder().consumeMessage((m,h) -> m.getChannel()).ifLastConsumeEvent().isOlderThan(7).minutes().buildFeature();
		
		assertNotNull("object should not be  null",feature);
		ruleList = ((MessageConsumerFeature.MessageConsumerFeatureConfiguration)feature).getConsumerRuleList();
		assertEquals("value should be correct", 1, ruleList.size());
		consumerRule = ruleList.get(0);
		assertNotNull("value should be correct", consumerRule.getMessageConsumer());
		assertEquals("value should be correct",TriggerByMessageAgeMode.NONE, consumerRule.getMessageAgeTriggerMode());
		assertEquals("value should be correct",-1, consumerRule.getMessageAgeTriggerCount());
		assertEquals("value should be correct",-1, consumerRule.getMessageAgeTriggerAge());
		assertEquals("value should be correct",TimeUnit.SECONDS, consumerRule.getMessageAgeTriggerUnit());
		assertEquals("value should be correct", "CONSUMER_PRIVATE_GROUP_" + consumerRule.getId().toString() , consumerRule.getConsumeEventAgeTriggerGroup());
		assertEquals("value should be correct",7, consumerRule.getConsumeEventAgeTriggerAge());
		assertEquals("value should be correct",TimeUnit.MINUTES, consumerRule.getConsumeEventAgeTriggerUnit());
		
		feature = MessageConsumerFeature.newBuilder().consumeMessage((m,h) -> m.getChannel()).ifLastConsumeEvent().ofGroup("ABC").isOlderThan(7).minutes().buildFeature();
		
		assertNotNull("object should not be  null",feature);
		ruleList = ((MessageConsumerFeature.MessageConsumerFeatureConfiguration)feature).getConsumerRuleList();
		assertEquals("value should be correct", 1, ruleList.size());
		consumerRule = ruleList.get(0);
		assertNotNull("value should be correct", consumerRule.getMessageConsumer());
		assertEquals("value should be correct",TriggerByMessageAgeMode.NONE, consumerRule.getMessageAgeTriggerMode());
		assertEquals("value should be correct",-1, consumerRule.getMessageAgeTriggerCount());
		assertEquals("value should be correct",-1, consumerRule.getMessageAgeTriggerAge());
		assertEquals("value should be correct",TimeUnit.SECONDS, consumerRule.getMessageAgeTriggerUnit());
		assertEquals("value should be correct", "ABC", consumerRule.getConsumeEventAgeTriggerGroup());
		assertEquals("value should be correct",7, consumerRule.getConsumeEventAgeTriggerAge());
		assertEquals("value should be correct",TimeUnit.MINUTES, consumerRule.getConsumeEventAgeTriggerUnit());
		
		feature = MessageConsumerFeature.newBuilder().consumeMessage((m,h) -> m.getChannel()).ifAllMessagesAreOlderThan(3).hours().buildFeature();
		
		assertNotNull("object should not be  null",feature);
		ruleList = ((MessageConsumerFeature.MessageConsumerFeatureConfiguration)feature).getConsumerRuleList();
		assertEquals("value should be correct", 1, ruleList.size());
		consumerRule = ruleList.get(0);
		assertNotNull("value should be correct", consumerRule.getMessageConsumer());
		assertEquals("value should be correct",TriggerByMessageAgeMode.ALL, consumerRule.getMessageAgeTriggerMode());
		assertEquals("value should be correct",-1, consumerRule.getMessageAgeTriggerCount());
		assertEquals("value should be correct",3, consumerRule.getMessageAgeTriggerAge());
		assertEquals("value should be correct",TimeUnit.HOURS, consumerRule.getMessageAgeTriggerUnit());
		assertNull("value should be correct", consumerRule.getConsumeEventAgeTriggerGroup());
		assertEquals("value should be correct",-1, consumerRule.getConsumeEventAgeTriggerAge());
		assertEquals("value should be correct",TimeUnit.SECONDS, consumerRule.getConsumeEventAgeTriggerUnit());
		
		feature = MessageConsumerFeature
			.newBuilder().consumeMessage((m,h) -> m.getChannel())
				.ifAllMessagesAreOlderThan(77).minutes()
				.andIfLastConsumeEvent().isOlderThan(1).hours()
			.buildFeature();
		
		assertNotNull("object should not be  null",feature);
		ruleList = ((MessageConsumerFeature.MessageConsumerFeatureConfiguration)feature).getConsumerRuleList();
		assertEquals("value should be correct", 1, ruleList.size());
		consumerRule = ruleList.get(0);
		assertNotNull("value should be correct", consumerRule.getMessageConsumer());
		assertEquals("value should be correct",TriggerByMessageAgeMode.ALL, consumerRule.getMessageAgeTriggerMode());
		assertEquals("value should be correct",-1, consumerRule.getMessageAgeTriggerCount());
		assertEquals("value should be correct",77, consumerRule.getMessageAgeTriggerAge());
		assertEquals("value should be correct",TimeUnit.MINUTES, consumerRule.getMessageAgeTriggerUnit());
		assertEquals("value should be correct", "CONSUMER_PRIVATE_GROUP_" + consumerRule.getId().toString() , consumerRule.getConsumeEventAgeTriggerGroup());
		assertEquals("value should be correct",1, consumerRule.getConsumeEventAgeTriggerAge());
		assertEquals("value should be correct",TimeUnit.HOURS, consumerRule.getConsumeEventAgeTriggerUnit());
		
		feature = MessageConsumerFeature.newBuilder().consumeMessage((m,h) -> m.getChannel()).ifAtLeastOneOfMessagesIsOlderThan(3).hours().buildFeature();
		
		assertNotNull("object should not be  null",feature);
		ruleList = ((MessageConsumerFeature.MessageConsumerFeatureConfiguration)feature).getConsumerRuleList();
		assertEquals("value should be correct", 1, ruleList.size());
		consumerRule = ruleList.get(0);
		assertNotNull("value should be correct", consumerRule.getMessageConsumer());
		assertEquals("value should be correct",TriggerByMessageAgeMode.LEAST_ONE, consumerRule.getMessageAgeTriggerMode());
		assertEquals("value should be correct",1, consumerRule.getMessageAgeTriggerCount());
		assertEquals("value should be correct",3, consumerRule.getMessageAgeTriggerAge());
		assertEquals("value should be correct",TimeUnit.HOURS, consumerRule.getMessageAgeTriggerUnit());
		assertNull("value should be correct", consumerRule.getConsumeEventAgeTriggerGroup());
		assertEquals("value should be correct",-1, consumerRule.getConsumeEventAgeTriggerAge());
		assertEquals("value should be correct",TimeUnit.SECONDS, consumerRule.getConsumeEventAgeTriggerUnit());
		
		feature = MessageConsumerFeature.newBuilder()
			.consumeMessage((m,h) -> m.getChannel())
				.ifAtLeastOneOfMessagesIsOlderThan(77).minutes()
				.andIfLastConsumeEvent().isOlderThan(1).hours()
			.buildFeature();
		
		assertNotNull("object should not be  null",feature);
		ruleList = ((MessageConsumerFeature.MessageConsumerFeatureConfiguration)feature).getConsumerRuleList();
		assertEquals("value should be correct", 1, ruleList.size());
		consumerRule = ruleList.get(0);
		assertNotNull("value should be correct", consumerRule.getMessageConsumer());
		assertEquals("value should be correct",TriggerByMessageAgeMode.LEAST_ONE, consumerRule.getMessageAgeTriggerMode());
		assertEquals("value should be correct",1, consumerRule.getMessageAgeTriggerCount());
		assertEquals("value should be correct",77, consumerRule.getMessageAgeTriggerAge());
		assertEquals("value should be correct",TimeUnit.MINUTES, consumerRule.getMessageAgeTriggerUnit());
		assertEquals("value should be correct", "CONSUMER_PRIVATE_GROUP_" + consumerRule.getId().toString() , consumerRule.getConsumeEventAgeTriggerGroup());
		assertEquals("value should be correct",1, consumerRule.getConsumeEventAgeTriggerAge());
		assertEquals("value should be correct",TimeUnit.HOURS, consumerRule.getConsumeEventAgeTriggerUnit());
		assertEquals("value should be correct",-1, consumerRule.getTimeOut());
		assertEquals("value should be correct",TimeUnit.SECONDS, consumerRule.getTimeOutUnit());
		
		feature = MessageConsumerFeature.newBuilder()
			.consumeMessage((m,h) -> m.getChannel()).withTimeoutForEachMessage(3).days()
				.ifAtLeastOneOfMessagesIsOlderThan(77).minutes()
				.andIfLastConsumeEvent().isOlderThan(1).hours()
			.buildFeature();
			
		assertNotNull("object should not be  null",feature);
		ruleList = ((MessageConsumerFeature.MessageConsumerFeatureConfiguration)feature).getConsumerRuleList();
		assertEquals("value should be correct", 1, ruleList.size());
		consumerRule = ruleList.get(0);
		assertNotNull("value should be correct", consumerRule.getMessageConsumer());
		assertEquals("value should be correct",TriggerByMessageAgeMode.LEAST_ONE, consumerRule.getMessageAgeTriggerMode());
		assertEquals("value should be correct",1, consumerRule.getMessageAgeTriggerCount());
		assertEquals("value should be correct",77, consumerRule.getMessageAgeTriggerAge());
		assertEquals("value should be correct",TimeUnit.MINUTES, consumerRule.getMessageAgeTriggerUnit());
		assertEquals("value should be correct", "CONSUMER_PRIVATE_GROUP_" + consumerRule.getId().toString() , consumerRule.getConsumeEventAgeTriggerGroup());
		assertEquals("value should be correct",1, consumerRule.getConsumeEventAgeTriggerAge());
		assertEquals("value should be correct",TimeUnit.HOURS, consumerRule.getConsumeEventAgeTriggerUnit());
		assertEquals("value should be correct",3, consumerRule.getTimeOut());
		assertEquals("value should be correct",TimeUnit.DAYS, consumerRule.getTimeOutUnit());
				
	}
	
	@Test
	public void test00004_BuilderSynthaxPhase4()
	{
		IChannelFeature feature = MessageConsumerFeature.newBuilder()
				.consumeMessage((m,h) -> m.getChannel()).immediately()
				.onError(TimeoutException.class, (e,h) -> { e.getMessage(); h.getMessage().getPayload().split("\\.");}  , String.class)
				.onError((e,h) -> {e.getMessage(); h.getMessage().getPayload().split("\\."); h.getHelper(() -> new ArrayList<>()).size();}, String.class , List.class)
				.onTimeout((m,h) -> m.removeFromChannel())
			.buildFeature();
		
		assertNotNull("object should not be  null",feature);
		List<ConsumerRule> ruleList = ((MessageConsumerFeature.MessageConsumerFeatureConfiguration)feature).getConsumerRuleList();
		assertEquals("value should be correct", 1, ruleList.size());
		ConsumerRule consumerRule = ruleList.get(0);
		assertNotNull("value should be correct", consumerRule.getMessageConsumer());
		assertEquals("value should be correct", 1, consumerRule.getSpecialErrorHandler().size());
		assertTrue("value should be correct", consumerRule.getSpecialErrorHandler().containsKey(TimeoutException.class));
		assertNotNull("value should be correct", consumerRule.getDefaultErrorHandler());
		assertNotNull("value should be correct", consumerRule.getTimeOutHandler());
		assertFalse("value should be correct", consumerRule.isKeepMessages());
		
		feature = MessageConsumerFeature.newBuilder()
				.consumeMessage((m,h) -> m.getChannel()).immediately()
				.butKeepMessagesInChannel().andYesIKnowWhatThisMeans()
			.buildFeature();
		
		assertNotNull("object should not be  null",feature);
		ruleList = ((MessageConsumerFeature.MessageConsumerFeatureConfiguration)feature).getConsumerRuleList();
		assertEquals("value should be correct", 1, ruleList.size());
		consumerRule = ruleList.get(0);
		assertNotNull("value should be correct", consumerRule.getMessageConsumer());
		assertEquals("value should be correct", 0, consumerRule.getSpecialErrorHandler().size());
		assertNull("value should be correct", consumerRule.getDefaultErrorHandler());
		assertNull("value should be correct", consumerRule.getTimeOutHandler());
		assertTrue("value should be correct", consumerRule.isKeepMessages());
	}
	
	@Test
	public void test00005_BuilderSynthaxMultipleMonitors()
	{
		IChannelFeature feature = MessageConsumerFeature.newBuilder()
				.inMessageMonitoringPool().minPoolSize(17).consumeMessage((m,h) -> m.getChannel())
				.ifAtLeastOneOfMessagesIsOlderThan(13).seconds()
			.or()
				.inMessageMonitoringPool().maxPoolSize(1).consumeMessage((m,h) -> m.getChannel())
				.ifLastConsumeEvent().isOlderThan(33).seconds()
			.buildFeature();
		
		assertNotNull("object should not be  null",feature);
		List<ConsumerRule> ruleList = ((MessageConsumerFeature.MessageConsumerFeatureConfiguration)feature).getConsumerRuleList();
		assertEquals("value should be correct", 2, ruleList.size());
		ConsumerRule consumerRule = ruleList.get(0);
		assertNotNull("value should be correct", consumerRule.getMessageConsumer());
		assertEquals("value should be correct",TriggerByMessageAgeMode.LEAST_ONE, consumerRule.getMessageAgeTriggerMode());
		assertEquals("value should be correct",1, consumerRule.getMessageAgeTriggerCount());
		assertEquals("value should be correct",13, consumerRule.getMessageAgeTriggerAge());
		assertEquals("value should be correct",TimeUnit.SECONDS, consumerRule.getMessageAgeTriggerUnit());
		assertNull("value should be correct", consumerRule.getConsumeEventAgeTriggerGroup());
		assertEquals("value should be correct",-1, consumerRule.getConsumeEventAgeTriggerAge());
		assertEquals("value should be correct",TimeUnit.SECONDS, consumerRule.getConsumeEventAgeTriggerUnit());
		consumerRule = ruleList.get(1);
		assertNotNull("value should be correct", consumerRule.getMessageConsumer());
		assertEquals("value should be correct",TriggerByMessageAgeMode.NONE, consumerRule.getMessageAgeTriggerMode());
		assertEquals("value should be correct",-1, consumerRule.getMessageAgeTriggerCount());
		assertEquals("value should be correct",-1, consumerRule.getMessageAgeTriggerAge());
		assertEquals("value should be correct",TimeUnit.SECONDS, consumerRule.getMessageAgeTriggerUnit());
		assertEquals("value should be correct", "CONSUMER_PRIVATE_GROUP_" + consumerRule.getId().toString() , consumerRule.getConsumeEventAgeTriggerGroup());
		assertEquals("value should be correct",33, consumerRule.getConsumeEventAgeTriggerAge());
		assertEquals("value should be correct",TimeUnit.SECONDS, consumerRule.getConsumeEventAgeTriggerUnit());
		
	}
}
