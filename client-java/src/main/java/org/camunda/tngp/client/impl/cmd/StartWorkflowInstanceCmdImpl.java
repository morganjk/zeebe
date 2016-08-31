package org.camunda.tngp.client.impl.cmd;

import org.camunda.tngp.client.cmd.StartWorkflowInstanceCmd;
import org.camunda.tngp.client.cmd.WorkflowInstance;
import org.camunda.tngp.client.impl.ClientCmdExecutor;
import org.camunda.tngp.client.impl.cmd.wf.start.StartWorkflowInstanceRequestWriter;

public class StartWorkflowInstanceCmdImpl extends AbstractCmdImpl<WorkflowInstance>
    implements StartWorkflowInstanceCmd
{
    protected StartWorkflowInstanceRequestWriter requestWriter = new StartWorkflowInstanceRequestWriter();

    public StartWorkflowInstanceCmdImpl(final ClientCmdExecutor clientCmdExecutor)
    {
        super(clientCmdExecutor, new StartWorkflowInstanceResponseHandler());
    }

    @Override
    public StartWorkflowInstanceCmd workflowDefinitionId(long workflowTypeId)
    {
        requestWriter.wfDefinitionId(workflowTypeId);
        return this;
    }

    @Override
    public StartWorkflowInstanceCmd workflowDefinitionKey(byte[] key)
    {
        requestWriter.wfDefinitionKey(key);
        return this;
    }

    @Override
    public StartWorkflowInstanceCmd workflowDefinitionKey(String key)
    {
        return workflowDefinitionKey(key.getBytes(CHARSET));
    }

    @Override
    public ClientRequestWriter getRequestWriter()
    {
        return requestWriter;
    }

    public void setRequestWriter(StartWorkflowInstanceRequestWriter requestWriter)
    {
        this.requestWriter = requestWriter;
    }

}