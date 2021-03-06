package com.meltwater.rxrabbit;

import com.meltwater.rxrabbit.impl.DefaultChannelFactory;
import com.meltwater.rxrabbit.impl.RoundRobinPublisher;
import com.meltwater.rxrabbit.impl.SingleChannelPublisher;
import com.meltwater.rxrabbit.util.Logger;
import rx.Scheduler;
import rx.schedulers.Schedulers;

import java.util.ArrayList;
import java.util.List;

/**
 * Can create {@link RabbitPublisher}s.
 *
 * Note that a single {@link ChannelFactory} is backing all the {@link RabbitPublisher}s created by this factory.
 *
 * So if the {@link DefaultChannelFactory} is used it means that all publishers will share the same {@link com.rabbitmq.client.Connection}
 * but use different {@link com.rabbitmq.client.Channel}s.
 */
public class DefaultPublisherFactory implements PublisherFactory{

    private final Logger log = new Logger(DefaultPublisherFactory.class);

    private final PublisherSettings settings;
    private final ChannelFactory channelFactory;

    private PublishEventListener publishEventListener = getPublishEventListener();

    private Scheduler observeOnScheduler = Schedulers.computation();

    public DefaultPublisherFactory(ChannelFactory channelFactory, PublisherSettings settings) {
        assert settings!=null;
        assert channelFactory!=null;
        this.channelFactory = channelFactory;
        this.settings = settings;
    }

    public DefaultPublisherFactory setObserveOnScheduler(Scheduler observeOnScheduler) {
        assert observeOnScheduler!=null;
        this.observeOnScheduler = observeOnScheduler;
        return this;
    }

    public DefaultPublisherFactory setPublishEventListener(PublishEventListener publishEventListener) {
        assert publishEventListener!=null;
        this.publishEventListener = publishEventListener;
        return this;
    }

    @Override
    public RabbitPublisher createPublisher() {
        assert settings.getNum_channels()>0;
        log.infoWithParams("Creating publisher.",
                "publishChannels", settings.getNum_channels(),
                "publisherConfirms", settings.isPublisher_confirms(),
                "publishEventListener", publishEventListener);
        List<RabbitPublisher> publishers = new ArrayList<>();
        for(int i=0; i<settings.getNum_channels(); i++){
            publishers.add(new SingleChannelPublisher(
                    channelFactory,
                    settings.isPublisher_confirms(),
                    settings.getRetry_count(),
                    observeOnScheduler,
                    publishEventListener,
                    settings.getPublish_timeout_secs(),
                    settings.getClose_timeout_millis(),
                    1,
                    settings.getBackoff_algorithm()));
        }
        return new RoundRobinPublisher(publishers);
    }

    protected PublishEventListener getPublishEventListener() {
        return new NoopPublishEventListener();
    }

}
