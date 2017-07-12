package io.zeebe.transport;

import static org.assertj.core.api.Assertions.assertThat;

import org.agrona.DirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import io.zeebe.dispatcher.Dispatcher;
import io.zeebe.dispatcher.Dispatchers;
import io.zeebe.test.util.agent.ControllableTaskScheduler;

public class ControllableServerTransportTest
{
    protected static final SocketAddress ADDRESS = new SocketAddress("127.0.0.1", 51115);

    public static final DirectBuffer BUF1 = new UnsafeBuffer(new byte[32]);

    public static final int SEND_BUFFER_SIZE = 16 * 1024;
    public static final int MESSAGES_REQUIRED_TO_SATURATE_SEND_BUFFER = SEND_BUFFER_SIZE / BUF1.capacity();

    protected ServerTransport serverTransport;

    @Rule
    public ControllableTaskScheduler scheduler = new ControllableTaskScheduler();

    @Before
    public void setUp()
    {
        final Dispatcher sendBuffer = Dispatchers.create("clientSendBuffer")
            .bufferSize(SEND_BUFFER_SIZE)
            .subscriptions("sender")
            .actorScheduler(scheduler)
            .build();

        serverTransport = Transports.newServerTransport()
            .bindAddress(ADDRESS.toInetSocketAddress())
            .sendBuffer(sendBuffer)
            .scheduler(scheduler)
            .build(null, null);
    }

    @After
    public void tearDown()
    {
        serverTransport.closeAsync();
        scheduler.waitUntilDone();
    }

    @Test
    public void shouldRejectMessageWhenSendBufferSaturated()
    {
        // given
        final ServerOutput output = serverTransport.getOutput();
        final TransportMessage message = new TransportMessage()
            .buffer(BUF1)
            .remoteStreamId(0);

        for (int i = 0; i < MESSAGES_REQUIRED_TO_SATURATE_SEND_BUFFER; i++)
        {
            output.sendMessage(message);
        }

        // when
        final boolean success = output.sendMessage(message);

        // then
        assertThat(success).isFalse();
    }

    @Test
    public void shouldRejectResponseWhenSendBufferSaturated()
    {
        // given
        final ServerOutput output = serverTransport.getOutput();
        final ServerResponse response = new ServerResponse()
            .buffer(BUF1)
            .remoteStreamId(0)
            .requestId(1L);

        for (int i = 0; i < MESSAGES_REQUIRED_TO_SATURATE_SEND_BUFFER; i++)
        {
            output.sendResponse(response);
        }

        // when
        final boolean success = output.sendResponse(response);

        // then
        assertThat(success).isFalse();
    }


}
