package org.sodeac.common.message.service.api;

import org.sodeac.common.message.service.api.IServiceChannel.IChannelPolicy;

public interface ICommonChannelPolicies
{
    // prefetch
    interface IPreMessageRequest extends IChannelPolicy
    {
        IPreMessageRequest ifChannelMessageSizeLessThen(int value);
        
        IPreMessageRequest thenPreRequestForNext(int value);
        
        /**
         * dummy method for nicer syntax
         */
        void messages();
        
        // TODO  Force-Sythax-Builder
    }
}
