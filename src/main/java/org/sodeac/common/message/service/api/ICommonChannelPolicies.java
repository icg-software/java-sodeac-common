package org.sodeac.common.message.service.api;

import org.sodeac.common.message.service.api.IServiceChannel.IChannelPolicy;

public interface ICommonChannelPolicies
{
    // prefetch
    public interface IPreMessageRequest extends IChannelPolicy
    {
        public IPreMessageRequest ifChannelMessageSizeLessThen(int value);
        
        public IPreMessageRequest thenPreRequestForNext(int value);
        
        /**
         * dummy method for nicer syntax
         */
        public void messages();
        
        // TODO  Force-Sythax-Builder
    }
}
