/*
 *
 *  Copyright 2016 Netflix, Inc.
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */
package com.netflix.genie.web.configs;

import com.netflix.genie.common.exceptions.GenieException;
import com.netflix.genie.core.jobs.workflow.WorkflowTask;
import com.netflix.genie.core.jpa.repositories.JpaApplicationRepository;
import com.netflix.genie.core.jpa.repositories.JpaClusterRepository;
import com.netflix.genie.core.jpa.repositories.JpaCommandRepository;
import com.netflix.genie.core.jpa.repositories.JpaJobExecutionRepository;
import com.netflix.genie.core.jpa.repositories.JpaJobRepository;
import com.netflix.genie.core.jpa.repositories.JpaJobRequestRepository;
import com.netflix.genie.core.services.ApplicationService;
import com.netflix.genie.core.services.ClusterLoadBalancer;
import com.netflix.genie.core.services.ClusterService;
import com.netflix.genie.core.services.CommandService;
import com.netflix.genie.core.services.FileTransfer;
import com.netflix.genie.core.services.JobKillService;
import com.netflix.genie.core.services.JobPersistenceService;
import com.netflix.genie.core.services.JobSearchService;
import com.netflix.genie.core.services.JobSubmitterService;
import com.netflix.genie.core.services.impl.GenieFileTransferService;
import com.netflix.genie.test.categories.UnitTest;
import org.apache.commons.exec.Executor;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mockito;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.Resource;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.ArrayList;
import java.util.List;

/**
 * Unit Tests for ServicesConfig class.
 *
 * @author amsharma
 * @since 3.0.0
 */
@Category(UnitTest.class)
public class ServicesConfigUnitTests {

    private JpaApplicationRepository applicationRepository;
    private JpaClusterRepository clusterRepository;
    private JpaCommandRepository commandRepository;
    private JpaJobExecutionRepository jobExecutionRepository;
    private JpaJobRepository jobRepository;
    private JpaJobRequestRepository jobRequestRepository;
    private JobSearchService jobSearchService;
    private ServicesConfig servicesConfig;

    /**
     * Setup to run before each test.
     */
    @Before
    public void setUp() {
        this.applicationRepository = Mockito.mock(JpaApplicationRepository.class);
        this.clusterRepository = Mockito.mock(JpaClusterRepository.class);
        this.commandRepository = Mockito.mock(JpaCommandRepository.class);
        this.jobRepository = Mockito.mock(JpaJobRepository.class);
        this.jobRequestRepository = Mockito.mock(JpaJobRequestRepository.class);
        this.jobExecutionRepository = Mockito.mock(JpaJobExecutionRepository.class);
        this.jobSearchService = Mockito.mock(JobSearchService.class);

        this.servicesConfig = new ServicesConfig();
    }

    /**
     * Confirm we can get a cluster load balancer.
     */
    @Test
    public void canGetClusterLoadBalancer() {
        Assert.assertNotNull(this.servicesConfig.clusterLoadBalancer());
    }

    /**
     * Confirm we can get a GenieFileTransfer instance.
     *
     * @throws GenieException If there is any problem.
     */
    @Test
    public void canGetGenieFileTransfer() throws GenieException {
        final ArrayList<FileTransfer> fileTransferList = new ArrayList<>();
        Assert.assertNotNull(this.servicesConfig.genieFileTransferService(fileTransferList));
    }

    /**
     * Confirm we can get a default mail service implementation.
     */
    @Test
    public void canGetDefaultMailServiceImpl() {
        Assert.assertNotNull(this.servicesConfig.getDefaultMailServiceImpl());
    }

    /**
     * Confirm we can get a mail service implementation using JavaMailSender.
     *
     * @throws GenieException if there is any problem.
     */
    @Test
    public void canGetMailServiceImpl() throws GenieException {
        final JavaMailSender javaMailSender = Mockito.mock(JavaMailSender.class);
        Assert.assertNotNull(
            this.servicesConfig.getJavaMailSenderMailService(
                javaMailSender,
                "fromAddress",
                "user",
                "password"
            )
        );
    }

    /**
     * Confirm we can get a GenieNodeStatistics implementation.
     */
    @Test
    public void canGetGenieNodeStatisticsBean() {
        Assert.assertNotNull(this.servicesConfig.getGenieNodeStatistics());
    }

    /**
     * Can get a bean for Application Service.
     */
    @Test
    public void canGetApplicationServiceBean() {

        Assert.assertNotNull(
            this.servicesConfig.applicationService(
                this.applicationRepository,
                this.commandRepository
            )
        );
    }

    /**
     * Can get a bean for Command Service.
     */
    @Test
    public void canGetCommandServiceBean() {

        Assert.assertNotNull(
            this.servicesConfig.commandService(
                this.commandRepository,
                this.applicationRepository,
                this.clusterRepository
            )
        );
    }

    /**
     * Can get a bean for Cluster Service.
     */
    @Test
    public void canGetClusterServiceBean() {
        Assert.assertNotNull(
            this.servicesConfig.clusterService(
                this.clusterRepository,
                this.commandRepository
            )
        );
    }

    /**
     * Can get a bean for Job Search Service.
     */
    @Test
    public void canGetJobSearchServiceBean() {
        Assert.assertNotNull(
            this.servicesConfig.jobSearchService(
                this.jobRepository,
                this.jobRequestRepository,
                this.jobExecutionRepository
            )
        );
    }

    /**
     * Can get a bean for Job Persistence Service.
     */
    @Test
    public void canGetJobPersistenceServiceBean() {
        Assert.assertNotNull(
            this.servicesConfig.jobPersistenceService(
                this.jobRepository,
                this.jobRequestRepository,
                this.jobExecutionRepository,
                this.applicationRepository,
                this.clusterRepository,
                this.commandRepository
            )
        );
    }

    /**
     * Can get a bean for Job Submitter Service.
     */
    @Test
    public void canGetJobSubmitterServiceBean() {
        final JobPersistenceService jobPersistenceService = Mockito.mock(JobPersistenceService.class);
        final ApplicationService applicationService = Mockito.mock(ApplicationService.class);
        final ClusterService clusterService = Mockito.mock(ClusterService.class);
        final CommandService commandService = Mockito.mock(CommandService.class);
        final ClusterLoadBalancer clusterLoadBalancer = Mockito.mock(ClusterLoadBalancer.class);
        final GenieFileTransferService genieFileTransferService = Mockito.mock(GenieFileTransferService.class);
        final ApplicationEventPublisher applicationEventPublisher = Mockito.mock(ApplicationEventPublisher.class);
        final Resource resource = Mockito.mock(Resource.class);
        final List<WorkflowTask> workflowTasks = new ArrayList<>();

        Assert.assertNotNull(
            this.servicesConfig.jobSubmitterService(
                this.jobSearchService,
                jobPersistenceService,
                applicationService,
                clusterService,
                commandService,
                clusterLoadBalancer,
                genieFileTransferService,
                applicationEventPublisher,
                workflowTasks,
                resource,
                "localhost",
                5
            )
        );
    }

    /**
     * Can get a bean for Job Coordinator Service.
     */
    @Test
    public void canGetJobCoordinatorServiceBean() {

        final JobPersistenceService jobPersistenceService = Mockito.mock(JobPersistenceService.class);
        final JobSubmitterService jobSubmitterService = Mockito.mock(JobSubmitterService.class);
        final JobKillService jobKillService = Mockito.mock(JobKillService.class);

        Assert.assertNotNull(
            this.servicesConfig.jobCoordinatorService(
                jobPersistenceService,
                this.jobSearchService,
                jobSubmitterService,
                jobKillService,
                "file:///tmp"
            )
        );
    }

    /**
     * Can get a bean for Job Kill Service.
     */
    @Test
    public void canGetJobKillServiceBean() {
        Assert.assertNotNull(
            this.servicesConfig.jobKillService(
                "localhost",
                this.jobSearchService,
                Mockito.mock(Executor.class)
            )
        );
    }
}
