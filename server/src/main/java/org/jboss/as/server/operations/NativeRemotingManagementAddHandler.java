/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.server.operations;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ModelController;
import org.jboss.as.controller.ProcessType;
import org.jboss.as.controller.RunningMode;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.management.ManagementInterfaceAddStepHandler;
import org.jboss.as.controller.remote.AbstractModelControllerOperationHandlerFactoryService;
import org.jboss.as.controller.remote.ModelControllerClientOperationHandlerFactoryService;
import org.jboss.as.controller.remote.ModelControllerOperationHandlerFactory;
import org.jboss.as.remoting.RemotingServices;
import org.jboss.as.remoting.management.ManagementChannelRegistryService;
import org.jboss.as.remoting.management.ManagementRemotingServices;
import org.jboss.as.server.ServerService;
import org.jboss.as.server.Services;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;

/**
 * The Add handler for the Native Remoting Interface when running a standalone server.
 * (This reuses a connector from the remoting subsystem).
 *
 * @author Kabir Khan
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public class NativeRemotingManagementAddHandler extends ManagementInterfaceAddStepHandler {

    public static final NativeRemotingManagementAddHandler INSTANCE = new NativeRemotingManagementAddHandler();
    public static final String OPERATION_NAME = ModelDescriptionConstants.ADD;

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) {
        model.setEmptyObject();
    }


    @Override
    protected boolean requiresRuntime(OperationContext context) {
        return context.getProcessType() != ProcessType.EMBEDDED_SERVER || context.getRunningMode() != RunningMode.ADMIN_ONLY;
    }

    @Override
    public void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        final ServiceTarget serviceTarget = context.getServiceTarget();
        final ServiceName endpointName = RemotingServices.SUBSYSTEM_ENDPOINT;
        ManagementChannelRegistryService.addService(serviceTarget, endpointName);
        ManagementRemotingServices.installManagementChannelServices(serviceTarget,
                endpointName,
                new ModelControllerOperationHandlerFactory() {
                    @Override
                    public AbstractModelControllerOperationHandlerFactoryService newInstance(
                            final Consumer<AbstractModelControllerOperationHandlerFactoryService> serviceConsumer,
                            final Supplier<ModelController> modelControllerSupplier,
                            final Supplier<ExecutorService> executorSupplier,
                            final Supplier<ScheduledExecutorService> scheduledExecutorSupplier) {
                        return new ModelControllerClientOperationHandlerFactoryService(serviceConsumer, modelControllerSupplier, executorSupplier, scheduledExecutorSupplier);
                    }
                },
                Services.JBOSS_SERVER_CONTROLLER,
                ManagementRemotingServices.MANAGEMENT_CHANNEL,
                ServerService.EXECUTOR_CAPABILITY.getCapabilityServiceName(),
                ServerService.JBOSS_SERVER_SCHEDULED_EXECUTOR);
        List<ServiceName> requiredServices = Collections.singletonList(RemotingServices.channelServiceName(endpointName, ManagementRemotingServices.MANAGEMENT_CHANNEL));
        addVerifyInstallationStep(context, requiredServices);
    }

}
