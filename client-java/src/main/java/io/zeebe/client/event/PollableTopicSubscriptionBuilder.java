/*
 * Copyright © 2017 camunda services GmbH (info@camunda.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.zeebe.client.event;

/**
 * <p>Builder used to subscribed to all events of any kind of topic. Builds a <code>pollable</code> subscription,
 * i.e. where the method {@link PollableTopicSubscription#poll(TopicEventHandler)} must be invoked
 * to trigger event handling.
 *
 * <p>By default, a subscription starts at the current tail of the topic (see {@link #startAtTailOfTopic()}).
 *
 * <p>When an event handler invocation fails, invoking it is retried two times before the subscription is closed.
 */
public interface PollableTopicSubscriptionBuilder
{

    /**
     * Defines the position at which to start receiving events from.
     * A <code>position</code> greater than the current tail position
     * of the topic is equivalent to starting at the tail position. In this case,
     * events with a lower position than the supplied position may be received.
     *
     * @param position the position in the topic at which to start receiving events from
     * @return this builder
     */
    PollableTopicSubscriptionBuilder startAtPosition(long position);

    /**
     * Same as invoking {@link #startAtPosition(long)} with the topic's current tail position.
     * In particular, it is guaranteed that this subscription does not receive any event that
     * was receivable before this subscription is opened.
     *
     * @return this builder
     */
    PollableTopicSubscriptionBuilder startAtTailOfTopic();

    /**
     * Same as invoking {@link #startAtPosition(long)} with <code>position = 0</code>.
     *
     * @return this builder
     */
    PollableTopicSubscriptionBuilder startAtHeadOfTopic();

    /**
     * <p>Sets the name of a subscription. The name is used by the broker to record and persist the
     * subscription's position. When a subscription is reopened, this state is used to resume
     * the subscription at the previous position. In this case, methods like {@link #startAtPosition(long)}
     * have no effect (the subscription has already started before).
     *
     * <p>Example:
     * <pre>
     * PollableTopicSubscriptionBuilder builder = ...;
     * builder
     *   .startAtPosition(0)
     *   .name("app1")
     *   ...
     *   .open();
     * </pre>
     * When executed the first time, this snippet creates a new subscription beginning at position 0.
     * When executed a second time, this snippet creates a new subscription beginning at the position
     * at which the first subscription left off.
     *
     * <p>Use {@link #forcedStart()} to enforce starting at the supplied start position.
     *
     * <p>This parameter is required.
     *
     * @param name the name of the subscription. must be unique for the addressed topic
     * @return this builder
     */
    PollableTopicSubscriptionBuilder name(String subscriptionName);

    /**
     * Forces the subscription to start over, discarding any
     * state previously persisted in the broker. The next received events are based
     * on the configured start position.
     *
     * @return this builder
     */
    PollableTopicSubscriptionBuilder forcedStart();

    /**
     * TEMPORARY: Defines the partition to subscribe to.
     * If no partition id is set, opens a subscription to the single existing partition. An exception
     * is thrown if there is no such partition.
     */
    PollableTopicSubscriptionBuilder partitionId(int partition);

    /**
     * Opens a new topic subscription with the defined parameters.
     *
     * @return a new subscription
     */
    PollableTopicSubscription open();
}
