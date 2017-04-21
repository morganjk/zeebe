package org.camunda.tngp.broker.workflow.graph;

import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.tngp.broker.workflow.graph.model.*;
import org.camunda.tngp.broker.workflow.graph.transformer.BpmnTransformer;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.camunda.tngp.broker.workflow.graph.transformer.TngpExtensions.wrap;
import static org.camunda.tngp.test.util.BufferAssert.assertThatBuffer;
import static org.camunda.tngp.util.buffer.BufferUtil.wrapString;

public class BpmnTransformerTests
{
    private BpmnTransformer bpmnTransformer = new BpmnTransformer();

    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();

    @Test
    public void shouldTransformStartEvent()
    {
        // given
        final BpmnModelInstance bpmnModelInstance = Bpmn.createExecutableProcess()
            .startEvent("foo")
                .name("bar")
            .done();

        // when
        final ExecutableWorkflow process = transformSingleProcess(bpmnModelInstance);

        // then
        final ExecutableFlowElement element = process.findFlowElementById(wrapString("foo"));
        assertThat(element).isInstanceOf(ExecutableStartEvent.class);
        assertThat(element.getId()).isEqualTo(wrapString("foo"));
        assertThat(element.getName()).isEqualTo("bar");

        assertThat(process.getScopeStartEvent()).isEqualTo(element);
    }

    @Test
    public void shouldTransformSequenceFlow()
    {
        // given
        final BpmnModelInstance bpmnModelInstance = Bpmn.createExecutableProcess()
                .startEvent("a")
                .sequenceFlowId("to")
                .endEvent("b")
                .done();

        // when
        final ExecutableWorkflow process = transformSingleProcess(bpmnModelInstance);

        // then
        final ExecutableFlowElement element = process.findFlowElementById(wrapString("to"));
        assertThat(element).isInstanceOf(ExecutableSequenceFlow.class);

        final ExecutableSequenceFlow sequenceFlow = (ExecutableSequenceFlow) element;
        assertThat(sequenceFlow.getId()).isEqualTo(wrapString("to"));
        assertThat(sequenceFlow.getSourceNode().getId()).isEqualTo(wrapString("a"));
        assertThat(sequenceFlow.getTargetNode().getId()).isEqualTo(wrapString("b"));

        assertThat(sequenceFlow.getSourceNode().getOutgoingSequenceFlows()).hasSize(1).contains(sequenceFlow);
        assertThat(sequenceFlow.getTargetNode().getIncomingSequenceFlows()).hasSize(1).contains(sequenceFlow);
    }

    @Test
    public void shouldTransformEndEvent()
    {
        // given
        final BpmnModelInstance bpmnModelInstance = Bpmn.createExecutableProcess()
            .startEvent()
            .endEvent("foo")
                .name("bar")
            .done();

        // when
        final ExecutableWorkflow process = transformSingleProcess(bpmnModelInstance);

        // then
        final ExecutableFlowElement element = process.findFlowElementById(wrapString("foo"));
        assertThat(element).isInstanceOf(ExecutableEndEvent.class);
        assertThat(element.getId()).isEqualTo(wrapString("foo"));
        assertThat(element.getName()).isEqualTo("bar");
    }

    @Test
    public void shouldTransformServiceTask()
    {
        // given
        final Map<String, String> taskHeaders = new HashMap<>();
        taskHeaders.put("a", "b");
        taskHeaders.put("c", "d");

        final BpmnModelInstance bpmnModelInstance = wrap(Bpmn.createExecutableProcess()
            .startEvent()
            .serviceTask("foo")
                .name("bar")
            .done())
                .taskDefinition("foo", "test", 4)
                .taskHeaders("foo", taskHeaders);

        // when
        final ExecutableWorkflow process = transformSingleProcess(bpmnModelInstance);

        // then
        final ExecutableFlowElement element = process.findFlowElementById(wrapString("foo"));
        assertThat(element).isInstanceOf(ExecutableServiceTask.class);

        final ExecutableServiceTask serviceTask = (ExecutableServiceTask) element;
        assertThat(serviceTask.getId()).isEqualTo(wrapString("foo"));
        assertThat(serviceTask.getName()).isEqualTo("bar");

        assertThat(serviceTask.getTaskMetadata()).isNotNull();
        assertThatBuffer(serviceTask.getTaskMetadata().getTaskType()).hasBytes("test".getBytes());
        assertThat(serviceTask.getTaskMetadata().getRetries()).isEqualTo(4);
        assertThat(serviceTask.getTaskMetadata().getHeaders())
            .hasSize(2)
            .extracting(h -> tuple(h.getKey(), h.getValue()))
            .contains(tuple("a", "b"), tuple("c", "d"));
    }

    @Test
    public void shouldTransformTaskMapping()
    {
        // given
        final BpmnModelInstance bpmnModelInstance = wrap(Bpmn.createExecutableProcess()
            .startEvent()
            .serviceTask("foo")
            .name("bar")
            .done())
            .taskDefinition("foo", "test", 4)
            .ioMapping("foo", "$", "$.*");

        // when
        final ExecutableWorkflow process = transformSingleProcess(bpmnModelInstance);

        // then
        final ExecutableFlowElement element = process.findFlowElementById(wrapString("foo"));
        final ExecutableServiceTask serviceTask = (ExecutableServiceTask) element;

        assertThat(serviceTask.getIoMapping()).isNotNull();
        assertThat(serviceTask.getIoMapping().getInputQuery()).isNotNull();
        assertThat(serviceTask.getIoMapping().getInputQuery().isValid()).isTrue();

        assertThat(serviceTask.getIoMapping().getOutputQuery()).isNotNull();
        assertThat(serviceTask.getIoMapping().getOutputQuery().isValid()).isTrue();
    }

    @Test
    public void shouldNotTransformForInvalidTaskMapping()
    {
        // given
        final BpmnModelInstance bpmnModelInstance = wrap(Bpmn.createExecutableProcess()
            .startEvent()
            .serviceTask("foo")
            .name("bar")
            .done())
            .taskDefinition("foo", "test", 4)
            .ioMapping("foo", "foo", "$.*");

        // expect
        exceptionRule.expect(RuntimeException.class);
        exceptionRule.expectMessage("Mapping failed JSON Path Query is not valid! Reason: Unexpected json-path token LITERAL");

        // when
        transformSingleProcess(bpmnModelInstance);
    }

    protected ExecutableWorkflow transformSingleProcess(BpmnModelInstance bpmnModelInstance)
    {
        final List<ExecutableWorkflow> processes = bpmnTransformer.transform(bpmnModelInstance);

        assertThat(processes.size()).isEqualTo(1);

        return processes.get(0);
    }

}