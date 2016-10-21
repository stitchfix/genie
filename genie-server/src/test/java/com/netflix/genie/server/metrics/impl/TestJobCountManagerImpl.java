/*
 *
 * Copyright 2015 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.netflix.genie.server.metrics.impl;

import com.github.springtestdbunit.DbUnitTestExecutionListener;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.netflix.genie.common.exceptions.GenieException;
import com.netflix.genie.common.model.Job;
import com.netflix.genie.server.metrics.JobCountManager;
import com.netflix.genie.server.repository.jpa.JobRepository;
import com.netflix.genie.server.util.NetUtil;

import java.util.Calendar;
import javax.inject.Inject;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.transaction.annotation.Transactional;

/**
 * Basic tests for the JobCountManager.
 *
 * @author skrishnan
 * @author tgianos
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:genie-application-test.xml")
@TestExecutionListeners({
    DependencyInjectionTestExecutionListener.class,
    DirtiesContextTestExecutionListener.class,
    TransactionalTestExecutionListener.class,
    DbUnitTestExecutionListener.class
})
@Transactional
public class TestJobCountManagerImpl {

    @Inject
    private JobRepository jobRepo;

    @Inject
    private JobCountManager manager;

    /**
     * Test getting number of running jobs on one instance.
     *
     * @throws GenieException For any problem if there is any error during this test
     */
    @Test
    @DatabaseSetup("testNumInstanceJobs.xml")
    public void testNumInstanceJobs() throws GenieException {
        //Force the hostname of the jobs to be the machine running the build
        final String hostName = NetUtil.getHostName();
        for (final Job job : this.jobRepo.findAll()) {
            job.setHostName(hostName);
        }
        this.jobRepo.flush();

        final Calendar one = Calendar.getInstance();
        one.clear();
        one.set(2014, Calendar.JULY, 1, 16, 27, 38);

        final Calendar two = Calendar.getInstance();
        two.clear();
        two.set(2014, Calendar.JULY, 1, 16, 27, 39);

        final Calendar three = Calendar.getInstance();
        three.clear();
        three.set(2014, Calendar.JULY, 1, 16, 27, 40);

        Assert.assertEquals(2, this.manager.getNumInstanceJobs());
        Assert.assertEquals(2,
                this.manager.getNumInstanceJobs(
                        0L,
                        System.currentTimeMillis()
                )
        );
        Assert.assertEquals(1,
                this.manager.getNumInstanceJobs(
                        one.getTimeInMillis(),
                        two.getTimeInMillis()
                )
        );
        Assert.assertEquals(1,
                this.manager.getNumInstanceJobs(
                        hostName,
                        two.getTimeInMillis(),
                        three.getTimeInMillis()
                )
        );
        Assert.assertEquals(0, this.manager.getNumInstanceJobs(0L, 0L));
    }

    /**
     * Test getting total number of running jobs system wide.
     *
     * @throws GenieException For any problem if there is any error during this test
     */
    @Test
    @DatabaseSetup("testNumInstanceJobs2.xml")
    public void testGetNumRunningJobs() throws GenieException {
        Assert.assertEquals(3, this.manager.getNumRunningJobs());
    }

}
