/*
 * ModeShape (http://www.modeshape.org)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * ModeShape is free software. Unless otherwise indicated, all code in ModeShape
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * ModeShape is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.modeshape.jboss.metric;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import org.jboss.as.controller.AbstractRuntimeOnlyHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.msc.service.ServiceController;
import org.modeshape.common.util.CheckArg;
import org.modeshape.jboss.subsystem.ModeShapeServiceNames;
import org.modeshape.jcr.api.monitor.History;
import org.modeshape.jcr.api.monitor.RepositoryMonitor;
import org.modeshape.jcr.api.monitor.Statistics;
import org.modeshape.jcr.api.monitor.Window;

/**
 * Represents either a ModeShape value or duration metric operation.
 */
public abstract class ModeShapeMetricHandler extends AbstractRuntimeOnlyHandler {

    /**
     * Will be <code>null</code> until {@link #logger} is called.
     */
    private Logger logger;

    private final Window window;

    /**
     * @param metricWindow the metric window (cannot be <code>null</code>)
     */
    protected ModeShapeMetricHandler( final Window metricWindow ) {
        CheckArg.isNotNull(metricWindow, "metricWindow");
        this.window = metricWindow;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.as.controller.AbstractRuntimeOnlyHandler#executeRuntimeStep(org.jboss.as.controller.OperationContext,
     *      org.jboss.dmr.ModelNode)
     */
    @Override
    protected void executeRuntimeStep( final OperationContext context,
                                       final ModelNode operation ) throws OperationFailedException {
        final ModelNode address = operation.require(OP_ADDR);
        final PathAddress pathAddress = PathAddress.pathAddress(address);
        final String repositoryName = pathAddress.getLastElement().getValue();
        final ServiceController<?> sc = context.getServiceRegistry(false).getRequiredService(ModeShapeServiceNames.monitorServiceName(repositoryName));
        final RepositoryMonitor repoStats = (RepositoryMonitor)sc.getValue();

        try {
            final History history = history(repoStats);
            final Statistics[] stats = history.getStats();

            if ((stats.length != 0) && (stats[stats.length - 1] != null)) {
                final ModelNode result = context.getResult();

                for (final Statistics sample : stats) {
                    // sample can be null if the window is larger than the repository uptime
                    if (sample != null) {
                        result.add(sample.getMaximum());
                    }
                }
            }

            context.stepCompleted();
        } catch (final Exception e) {
            throw new OperationFailedException(e);
        }
    }

    /**
     * @param repoStats the repository statistics used to obtain the metric history (cannot be <code>null</code>)
     * @return the requested metric history (never <code>null</code> but can be empty)
     * @throws Exception if there is a problem obtaining the repository statistics history
     */
    protected abstract History history( final RepositoryMonitor repoStats ) throws Exception;

    /**
     * @return the logger (never <code>null</code>)
     */
    protected final Logger logger() {
        if (this.logger == null) {
            this.logger = Logger.getLogger(getClass().getName());
        }

        return this.logger;
    }

    /**
     * @return the metrics window (never <code>null</code>)
     */
    protected Window window() {
        return this.window;
    }

}
