/*
 *
 *  Copyright 2015 Netflix, Inc.
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
package com.netflix.genie.core.jpa.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonpatch.JsonPatch;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DatabaseTearDown;
import com.netflix.genie.common.dto.Cluster;
import com.netflix.genie.common.dto.ClusterStatus;
import com.netflix.genie.common.dto.Command;
import com.netflix.genie.common.dto.CommandStatus;
import com.netflix.genie.common.exceptions.GenieException;
import com.netflix.genie.core.services.ClusterService;
import com.netflix.genie.core.services.CommandService;
import com.netflix.genie.test.categories.IntegrationTest;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mapping.PropertyReferenceException;

import javax.validation.ConstraintViolationException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Integration tests for the CommandServiceJPAImpl.
 *
 * @author tgianos
 * @since 2.0.0
 */
@Category(IntegrationTest.class)
@DatabaseSetup("JpaClusterServiceImplIntegrationTests/init.xml")
@DatabaseTearDown("cleanup.xml")
public class JpaClusterServiceImplIntegrationTests extends DBUnitTestBase {

    private static final String COMMAND_1_ID = "command1";
    private static final String COMMAND_2_ID = "command2";
    private static final String COMMAND_3_ID = "command3";

    private static final String CLUSTER_1_ID = "cluster1";
    private static final String CLUSTER_1_USER = "tgianos";
    private static final String CLUSTER_1_NAME = "h2prod";
    private static final String CLUSTER_1_VERSION = "2.4.0";
    private static final ClusterStatus CLUSTER_1_STATUS = ClusterStatus.UP;

    private static final String CLUSTER_2_ID = "cluster2";
    private static final String CLUSTER_2_USER = "amsharma";
    private static final String CLUSTER_2_NAME = "h2query";
    private static final String CLUSTER_2_VERSION = "2.4.0";
    private static final ClusterStatus CLUSTER_2_STATUS = ClusterStatus.UP;

    private static final Pageable PAGE = new PageRequest(0, 10, Sort.Direction.DESC, "updated");

    @Autowired
    private ClusterService service;

    @Autowired
    private CommandService commandService;

    /**
     * Test the get cluster method.
     *
     * @throws GenieException For any problem
     */
    @Test
    public void testGetCluster() throws GenieException {
        final Cluster cluster1 = this.service.getCluster(CLUSTER_1_ID);
        Assert.assertEquals(CLUSTER_1_ID, cluster1.getId());
        Assert.assertEquals(CLUSTER_1_NAME, cluster1.getName());
        Assert.assertEquals(CLUSTER_1_USER, cluster1.getUser());
        Assert.assertEquals(CLUSTER_1_VERSION, cluster1.getVersion());
        Assert.assertEquals(CLUSTER_1_STATUS, cluster1.getStatus());
        Assert.assertEquals(5, cluster1.getTags().size());
        Assert.assertEquals(1, cluster1.getConfigs().size());

        final Cluster cluster2 = this.service.getCluster(CLUSTER_2_ID);
        Assert.assertEquals(CLUSTER_2_ID, cluster2.getId());
        Assert.assertEquals(CLUSTER_2_NAME, cluster2.getName());
        Assert.assertEquals(CLUSTER_2_USER, cluster2.getUser());
        Assert.assertEquals(CLUSTER_2_VERSION, cluster2.getVersion());
        Assert.assertEquals(CLUSTER_2_STATUS, cluster2.getStatus());
        Assert.assertEquals(5, cluster2.getTags().size());
        Assert.assertEquals(2, cluster2.getConfigs().size());
    }

    /**
     * Test the get cluster method.
     *
     * @throws GenieException For any problem
     */
    @Test(expected = ConstraintViolationException.class)
    public void testGetClusterNull() throws GenieException {
        this.service.getCluster(null);
    }

    /**
     * Test the get clusters method.
     */
    @Test
    public void testGetClustersByName() {
        final Page<Cluster> clusters = this.service.getClusters(CLUSTER_2_NAME, null, null, null, null, PAGE);
        Assert.assertEquals(1, clusters.getNumberOfElements());
        Assert.assertEquals(CLUSTER_2_ID, clusters.getContent().get(0).getId());
    }

    /**
     * Test the get clusters method.
     */
    @Test
    public void testGetClustersByStatuses() {
        final Set<ClusterStatus> statuses = EnumSet.noneOf(ClusterStatus.class);
        statuses.add(ClusterStatus.UP);
        final Page<Cluster> clusters = this.service.getClusters(null, statuses, null, null, null, PAGE);
        Assert.assertEquals(2, clusters.getNumberOfElements());
        Assert.assertEquals(CLUSTER_2_ID, clusters.getContent().get(0).getId());
        Assert.assertEquals(CLUSTER_1_ID, clusters.getContent().get(1).getId());
    }

    /**
     * Test the get clusters method.
     */
    @Test
    public void testGetClustersByTags() {
        final Set<String> tags = new HashSet<>();
        tags.add("prod");
        Page<Cluster> clusters = this.service.getClusters(null, null, tags, null, null, PAGE);
        Assert.assertEquals(1, clusters.getNumberOfElements());
        Assert.assertEquals(CLUSTER_1_ID, clusters.getContent().get(0).getId());

        tags.clear();
        tags.add("hive");
        clusters = this.service.getClusters(null, null, tags, null, null, PAGE);
        Assert.assertEquals(2, clusters.getNumberOfElements());
        Assert.assertEquals(CLUSTER_2_ID, clusters.getContent().get(0).getId());
        Assert.assertEquals(CLUSTER_1_ID, clusters.getContent().get(1).getId());

        tags.add("somethingThatWouldNeverReallyExist");
        clusters = this.service.getClusters(null, null, tags, null, null, PAGE);
        Assert.assertTrue(clusters.getContent().isEmpty());

        tags.clear();
        clusters = this.service.getClusters(null, null, tags, null, null, PAGE);
        Assert.assertEquals(2, clusters.getNumberOfElements());
        Assert.assertEquals(CLUSTER_2_ID, clusters.getContent().get(0).getId());
        Assert.assertEquals(CLUSTER_1_ID, clusters.getContent().get(1).getId());
    }

    /**
     * Test the get clusters method.
     */
    @Test
    public void testGetClustersByMinUpdateTime() {
        final Calendar time = Calendar.getInstance();
        time.clear();
        time.set(2014, Calendar.JULY, 9, 2, 58, 59);
        final Page<Cluster> clusters = this.service.getClusters(null, null, null, time.getTime(), null, PAGE);
        Assert.assertEquals(1, clusters.getNumberOfElements());
        Assert.assertEquals(CLUSTER_2_ID, clusters.getContent().get(0).getId());
    }

    /**
     * Test the get clusters method.
     */
    @Test
    public void testGetClustersByMaxUpdateTime() {
        final Calendar time = Calendar.getInstance();
        time.clear();
        time.set(2014, Calendar.JULY, 8, 3, 0, 0);
        final Page<Cluster> clusters = this.service.getClusters(null, null, null, null, time.getTime(), PAGE);
        Assert.assertEquals(1, clusters.getNumberOfElements());
        Assert.assertEquals(CLUSTER_1_ID, clusters.getContent().get(0).getId());
    }

    /**
     * Test the get clusters method with descending sort.
     */
    @Test
    public void testGetClustersDescending() {
        //Default to order by Updated
        final Page<Cluster> clusters = this.service.getClusters(null, null, null, null, null, PAGE);
        Assert.assertEquals(2, clusters.getNumberOfElements());
        Assert.assertEquals(CLUSTER_2_ID, clusters.getContent().get(0).getId());
        Assert.assertEquals(CLUSTER_1_ID, clusters.getContent().get(1).getId());
    }

    /**
     * Test the get clusters method with ascending sort.
     */
    @Test
    public void testGetClustersAscending() {
        final Pageable ascendingPage = new PageRequest(0, 10, Sort.Direction.ASC, "updated");
        //Default to order by Updated
        final Page<Cluster> clusters = this.service.getClusters(null, null, null, null, null, ascendingPage);
        Assert.assertEquals(2, clusters.getNumberOfElements());
        Assert.assertEquals(CLUSTER_1_ID, clusters.getContent().get(0).getId());
        Assert.assertEquals(CLUSTER_2_ID, clusters.getContent().get(1).getId());
    }

    /**
     * Test the get clusters method order by name.
     */
    @Test
    public void testGetClustersOrderBysUser() {
        final Pageable userPage = new PageRequest(0, 10, Sort.Direction.DESC, "user");
        final Page<Cluster> clusters = this.service.getClusters(null, null, null, null, null, userPage);
        Assert.assertEquals(2, clusters.getNumberOfElements());
        Assert.assertEquals(CLUSTER_1_ID, clusters.getContent().get(0).getId());
        Assert.assertEquals(CLUSTER_2_ID, clusters.getContent().get(1).getId());
    }

    /**
     * Test the get clusters method order by an invalid field should return the order by default value (updated).
     */
    @Test(expected = PropertyReferenceException.class)
    public void testGetClustersOrderBysInvalidField() {
        final Pageable badPage = new PageRequest(0, 10, Sort.Direction.DESC, "I'mNotAValidField");
        this.service.getClusters(null, null, null, null, null, badPage);
    }

    /**
     * Test the get clusters method order by a collection field should return the order by default value (updated).
     */
    @Ignore
    @Test
    public void testGetClustersOrderBysCollectionField() {
        final Pageable tagPage = new PageRequest(0, 10, Sort.Direction.DESC, "tags");
        final Page<Cluster> clusters = this.service.getClusters(null, null, null, null, null, tagPage);
        Assert.assertEquals(2, clusters.getNumberOfElements());
        Assert.assertEquals(CLUSTER_2_ID, clusters.getContent().get(0).getId());
        Assert.assertEquals(CLUSTER_1_ID, clusters.getContent().get(1).getId());
    }

    /**
     * Test the choseClusterForJob function.
     *
     * @throws GenieException For any problem
     */
    @Ignore
    @Test
    public void testChooseClusterForJob() throws GenieException {
//        final List<Cluster> clusters = this.service.chooseClusterForJob(JOB_1_ID);
//        Assert.assertEquals(1, clusters.size());
//        Assert.assertEquals(CLUSTER_1_ID, clusters.get(0).getId());
//        final JobEntity jobEntity = this.jpaJobRepository.findOne(JOB_1_ID);
//        final String chosen = jobEntity.getChosenClusterCriteriaString();
//        Assert.assertEquals(8, chosen.length());
//        Assert.assertTrue(chosen.contains("prod"));
//        Assert.assertTrue(chosen.contains("pig"));
//        Assert.assertTrue(chosen.contains(","));
    }

    // TODO Add tests where jobRequest object is

    /**
     * Test the create method.
     *
     * @throws GenieException For any problem
     */
    @Test
    public void testCreateCluster() throws GenieException {
        final Set<String> configs = new HashSet<>();
        configs.add("a config");
        configs.add("another config");
        configs.add("yet another config");
        final String id = UUID.randomUUID().toString();
        final Cluster cluster = new Cluster.Builder(
            CLUSTER_1_NAME,
            CLUSTER_1_USER,
            CLUSTER_1_VERSION,
            ClusterStatus.OUT_OF_SERVICE
        )
            .withId(id)
            .withConfigs(configs)
            .build();
        this.service.createCluster(cluster);
        final Cluster created = this.service.getCluster(id);
        Assert.assertNotNull(created);
        Assert.assertEquals(id, created.getId());
        Assert.assertEquals(CLUSTER_1_NAME, created.getName());
        Assert.assertEquals(CLUSTER_1_USER, created.getUser());
        Assert.assertEquals(ClusterStatus.OUT_OF_SERVICE, created.getStatus());
        Assert.assertEquals(3, created.getConfigs().size());
        this.service.deleteCluster(id);
        try {
            this.service.getCluster(id);
            Assert.fail("Should have thrown exception");
        } catch (final GenieException ge) {
            Assert.assertEquals(
                HttpURLConnection.HTTP_NOT_FOUND,
                ge.getErrorCode()
            );
        }
    }

    /**
     * Test the create method when no id is entered.
     *
     * @throws GenieException For any problem
     */
    @Test
    public void testCreateClusterNoId() throws GenieException {
        final Set<String> configs = new HashSet<>();
        configs.add("a config");
        configs.add("another config");
        configs.add("yet another config");
        final Cluster cluster = new Cluster.Builder(
            CLUSTER_1_NAME,
            CLUSTER_1_USER,
            CLUSTER_1_VERSION,
            ClusterStatus.OUT_OF_SERVICE
        )
            .withConfigs(configs)
            .build();
        final String id = this.service.createCluster(cluster);
        final Cluster created = this.service.getCluster(id);
        Assert.assertEquals(CLUSTER_1_NAME, created.getName());
        Assert.assertEquals(CLUSTER_1_USER, created.getUser());
        Assert.assertEquals(ClusterStatus.OUT_OF_SERVICE, created.getStatus());
        Assert.assertEquals(3, created.getConfigs().size());
        this.service.deleteCluster(created.getId());
        try {
            this.service.getCluster(created.getId());
            Assert.fail("Should have thrown exception");
        } catch (final GenieException ge) {
            Assert.assertEquals(
                HttpURLConnection.HTTP_NOT_FOUND,
                ge.getErrorCode()
            );
        }
    }

    /**
     * Test to make sure an exception is thrown when null is entered.
     *
     * @throws GenieException For any problem
     */
    @Test(expected = ConstraintViolationException.class)
    public void testCreateClusterNull() throws GenieException {
        this.service.createCluster(null);
    }

    /**
     * Test to update an cluster.
     *
     * @throws GenieException For any problem
     */
    @Test
    public void testUpdateClusterNoId() throws GenieException {
        final Cluster getCluster = this.service.getCluster(CLUSTER_1_ID);
        Assert.assertEquals(CLUSTER_1_USER, getCluster.getUser());
        Assert.assertEquals(ClusterStatus.UP, getCluster.getStatus());
        Assert.assertEquals(5, getCluster.getTags().size());

        final Set<String> tags = new HashSet<>(getCluster.getTags());
        tags.add("tez");
        tags.add("yarn");
        tags.add("hadoop");

        final Cluster updateCluster = new Cluster.Builder(
            getCluster.getName(),
            CLUSTER_2_USER,
            getCluster.getVersion(),
            ClusterStatus.OUT_OF_SERVICE
        )
            .withId(getCluster.getId())
            .withCreated(getCluster.getCreated())
            .withUpdated(getCluster.getUpdated())
            .withDescription(getCluster.getDescription())
            .withTags(tags)
            .withConfigs(getCluster.getConfigs())
            .build();
        this.service.updateCluster(CLUSTER_1_ID, updateCluster);

        final Cluster updated = this.service.getCluster(CLUSTER_1_ID);
        Assert.assertEquals(CLUSTER_2_USER, updated.getUser());
        Assert.assertEquals(ClusterStatus.OUT_OF_SERVICE, updated.getStatus());
        Assert.assertEquals(8, updated.getTags().size());
    }

    /**
     * Test to update an cluster with invalid content. Should throw ConstraintViolationException from JPA layer.
     *
     * @throws GenieException For any problem
     */
    @Test(expected = ConstraintViolationException.class)
    public void testUpdateClusterWithInvalidCluster() throws GenieException {
        final Cluster getCluster = this.service.getCluster(CLUSTER_1_ID);
        Assert.assertEquals(CLUSTER_1_USER, getCluster.getUser());
        Assert.assertEquals(ClusterStatus.UP, getCluster.getStatus());
        Assert.assertEquals(5, getCluster.getTags().size());

        final Cluster updateCluster = new Cluster.Builder(
            "", //invalid
            getCluster.getUser(),
            getCluster.getVersion(),
            ClusterStatus.OUT_OF_SERVICE
        )
            .withId(getCluster.getId())
            .build();

        this.service.updateCluster(CLUSTER_1_ID, updateCluster);
    }

    /**
     * Test to make sure setting the created and updated outside the system control doesn't change record in database.
     *
     * @throws GenieException For any problem
     */
    @Test
    public void testUpdateCreateAndUpdate() throws GenieException {
        final Cluster getCluster = this.service.getCluster(CLUSTER_1_ID);
        final Date created = getCluster.getCreated();
        final Date updated = getCluster.getUpdated();

        final Cluster updateCluster = new Cluster.Builder(
            getCluster.getName(),
            getCluster.getUser(),
            getCluster.getVersion(),
            getCluster.getStatus()
        )
            .withId(getCluster.getId())
            .withCreated(new Date())
            .withUpdated(new Date(0))
            .withDescription(getCluster.getDescription())
            .withTags(getCluster.getTags())
            .withConfigs(getCluster.getConfigs())
            .build();

        this.service.updateCluster(CLUSTER_1_ID, updateCluster);
        final Cluster updatedCluster = this.service.getCluster(CLUSTER_1_ID);
        Assert.assertEquals(created, updatedCluster.getCreated());
        Assert.assertNotEquals(updated, updatedCluster.getUpdated());
        Assert.assertNotEquals(new Date(0), updatedCluster.getUpdated());
    }

    /**
     * Test to update an cluster.
     *
     * @throws GenieException For any problem
     */
    @Test(expected = ConstraintViolationException.class)
    public void testUpdateClusterNullId() throws GenieException {
        this.service.updateCluster(null, this.service.getCluster(CLUSTER_1_ID));
    }

    /**
     * Test to update an cluster.
     *
     * @throws GenieException For any problem
     */
    @Test(expected = ConstraintViolationException.class)
    public void testUpdateClusterNullUpdateCluster() throws GenieException {
        this.service.updateCluster(CLUSTER_1_ID, null);
    }

    /**
     * Test to patch a cluster.
     *
     * @throws GenieException For any problem
     * @throws IOException For Json serialization problem
     */
    @Test
    public void testPatchCluster() throws GenieException, IOException {
        final Cluster getCluster = this.service.getCluster(CLUSTER_1_ID);
        Assert.assertThat(getCluster.getName(), Matchers.is(CLUSTER_1_NAME));
        final Date updateTime = getCluster.getUpdated();

        final String patchString
            = "[{ \"op\": \"replace\", \"path\": \"/name\", \"value\": \"" + CLUSTER_2_NAME + "\" }]";
        final ObjectMapper mapper = new ObjectMapper();
        final JsonPatch patch = JsonPatch.fromJson(mapper.readTree(patchString));

        this.service.patchCluster(CLUSTER_1_ID, patch);

        final Cluster updated = this.service.getCluster(CLUSTER_1_ID);
        Assert.assertNotEquals(updated.getUpdated(), Matchers.is(updateTime));
        Assert.assertThat(updated.getName(), Matchers.is(CLUSTER_2_NAME));
    }

    /**
     * Test delete all.
     *
     * @throws GenieException For any problem
     */
    @Test
    public void testDeleteAll() throws GenieException {
        Assert.assertEquals(2, this.service.getClusters(null, null, null, null, null, PAGE).getNumberOfElements());
        this.service.deleteAllClusters();
        Assert.assertTrue(this.service.getClusters(null, null, null, null, null, PAGE).getContent().isEmpty());
    }

    /**
     * Test delete.
     *
     * @throws GenieException For any problem
     */
    @Test
    public void testDelete() throws GenieException {
        Assert.assertEquals(2, this.commandService.getClustersForCommand(COMMAND_1_ID, null).size());
        Assert.assertEquals(2, this.commandService.getClustersForCommand(COMMAND_2_ID, null).size());
        Assert.assertEquals(2, this.commandService.getClustersForCommand(COMMAND_3_ID, null).size());

        this.service.deleteCluster(CLUSTER_1_ID);

        Assert.assertEquals(1, this.commandService.getClustersForCommand(COMMAND_1_ID, null).size());
        Assert.assertEquals(1,
            this.commandService.getClustersForCommand(COMMAND_1_ID, null)
                .stream()
                .filter(cluster -> CLUSTER_2_ID.equals(cluster.getId()))
                .count()
        );
        Assert.assertEquals(1, this.commandService.getClustersForCommand(COMMAND_2_ID, null).size());
        Assert.assertEquals(1,
            this.commandService.getClustersForCommand(COMMAND_2_ID, null)
                .stream()
                .filter(cluster -> CLUSTER_2_ID.equals(cluster.getId()))
                .count()
        );
        Assert.assertEquals(1, this.commandService.getClustersForCommand(COMMAND_3_ID, null).size());
        Assert.assertEquals(1,
            this.commandService.getClustersForCommand(COMMAND_3_ID, null)
                .stream()
                .filter(cluster -> CLUSTER_2_ID.equals(cluster.getId()))
                .count()
        );
    }

    /**
     * Test delete.
     *
     * @throws GenieException For any problem
     */
    @Test(expected = ConstraintViolationException.class)
    public void testDeleteNoId() throws GenieException {
        this.service.deleteCluster(null);
    }

    /**
     * Test add configurations to cluster.
     *
     * @throws GenieException For any problem
     */
    @Test
    public void testAddConfigsToCluster() throws GenieException {
        final String newConfig1 = UUID.randomUUID().toString();
        final String newConfig2 = UUID.randomUUID().toString();
        final String newConfig3 = UUID.randomUUID().toString();

        final Set<String> newConfigs = new HashSet<>();
        newConfigs.add(newConfig1);
        newConfigs.add(newConfig2);
        newConfigs.add(newConfig3);

        Assert.assertEquals(1, this.service.getConfigsForCluster(CLUSTER_1_ID).size());
        this.service.addConfigsForCluster(CLUSTER_1_ID, newConfigs);
        final Set<String> finalConfigs = this.service.getConfigsForCluster(CLUSTER_1_ID);
        Assert.assertEquals(4, finalConfigs.size());
        Assert.assertTrue(finalConfigs.contains(newConfig1));
        Assert.assertTrue(finalConfigs.contains(newConfig2));
        Assert.assertTrue(finalConfigs.contains(newConfig3));
    }

    /**
     * Test add configurations to cluster.
     *
     * @throws GenieException For any problem
     */
    @Test(expected = ConstraintViolationException.class)
    public void testAddConfigsToClusterNoId() throws GenieException {
        this.service.addConfigsForCluster(null, new HashSet<>());
    }

    /**
     * Test add configurations to cluster.
     *
     * @throws GenieException For any problem
     */
    @Test(expected = ConstraintViolationException.class)
    public void testAddConfigsToClusterNoConfigs() throws GenieException {
        this.service.addConfigsForCluster(CLUSTER_1_ID, null);
    }

    /**
     * Test update configurations for cluster.
     *
     * @throws GenieException For any problem
     */
    @Test
    public void testUpdateConfigsForCluster() throws GenieException {
        final String newConfig1 = UUID.randomUUID().toString();
        final String newConfig2 = UUID.randomUUID().toString();
        final String newConfig3 = UUID.randomUUID().toString();

        final Set<String> newConfigs = new HashSet<>();
        newConfigs.add(newConfig1);
        newConfigs.add(newConfig2);
        newConfigs.add(newConfig3);

        Assert.assertEquals(1, this.service.getConfigsForCluster(CLUSTER_1_ID).size());
        this.service.updateConfigsForCluster(CLUSTER_1_ID, newConfigs);
        final Set<String> finalConfigs = this.service.getConfigsForCluster(CLUSTER_1_ID);
        Assert.assertEquals(3, finalConfigs.size());
        Assert.assertTrue(finalConfigs.contains(newConfig1));
        Assert.assertTrue(finalConfigs.contains(newConfig2));
        Assert.assertTrue(finalConfigs.contains(newConfig3));
    }

    /**
     * Test update configurations for cluster.
     *
     * @throws GenieException For any problem
     */
    @Test(expected = ConstraintViolationException.class)
    public void testUpdateConfigsForClusterNoId() throws GenieException {
        this.service.updateConfigsForCluster(null, new HashSet<>());
    }

    /**
     * Test get configurations for cluster.
     *
     * @throws GenieException For any problem
     */
    @Test
    public void testGetConfigsForCluster() throws GenieException {
        Assert.assertEquals(1,
            this.service.getConfigsForCluster(CLUSTER_1_ID).size());
    }

    /**
     * Test get configurations to cluster.
     *
     * @throws GenieException For any problem
     */
    @Test(expected = ConstraintViolationException.class)
    public void testGetConfigsForClusterNoId() throws GenieException {
        this.service.getConfigsForCluster(null);
    }

    /**
     * Test adding commands to the cluster.
     *
     * @throws GenieException For any problem
     */
    @Test
    public void testAddCommandsForCluster() throws GenieException {
        final String command1Id = this.commandService.createCommand(
            new Command.Builder(
                "name",
                "user",
                "23.1.0",
                CommandStatus.ACTIVE,
                "pig",
                8108123L
            ).build()
        );
        final String command2Id = this.commandService.createCommand(
            new Command.Builder(
                "name2",
                "user2",
                "23.1.1",
                CommandStatus.INACTIVE,
                "pig2",
                8023423L
            ).build()
        );
        final List<String> newCommandIds = new ArrayList<>();
        newCommandIds.add(command1Id);
        newCommandIds.add(command2Id);
        Assert.assertEquals(3, this.service.getCommandsForCluster(CLUSTER_1_ID, null).size());
        this.service.addCommandsForCluster(CLUSTER_1_ID, newCommandIds);
        final List<Command> commands = this.service.getCommandsForCluster(CLUSTER_1_ID, null);
        Assert.assertEquals(5, commands.size());
        Assert.assertEquals(command1Id, commands.get(3).getId());
        Assert.assertEquals(command2Id, commands.get(4).getId());
    }

    /**
     * Test adding commands to the cluster.
     *
     * @throws GenieException For any problem
     */
    @Test(expected = ConstraintViolationException.class)
    public void testAddCommandsForClusterNoId() throws GenieException {
        this.service.addCommandsForCluster(null, new ArrayList<>());
    }

    /**
     * Test adding commands to the cluster.
     *
     * @throws GenieException For any problem
     */
    @Test(expected = ConstraintViolationException.class)
    public void testAddCommandsForClusterNoCommands() throws GenieException {
        this.service.addCommandsForCluster(UUID.randomUUID().toString(), null);
    }

    /**
     * Test the Get commands for cluster function.
     *
     * @throws GenieException For any problem
     */
    @Test
    public void testGetCommandsForCluster() throws GenieException {
        final List<Command> commands
            = this.service.getCommandsForCluster(CLUSTER_1_ID, null);
        Assert.assertEquals(3, commands.size());
        Assert.assertEquals(COMMAND_1_ID, commands.get(0).getId());
        Assert.assertEquals(COMMAND_3_ID, commands.get(1).getId());
        Assert.assertEquals(COMMAND_2_ID, commands.get(2).getId());
    }

    /**
     * Test the Get clusters for cluster function.
     *
     * @throws GenieException For any problem
     */
    @Test(expected = ConstraintViolationException.class)
    public void testGetCommandsForClusterNoId() throws GenieException {
        this.service.getCommandsForCluster("", null);
    }

    /**
     * Test updating commands for the cluster.
     *
     * @throws GenieException For any problem
     */
    @Test
    public void testUpdateCommandsForCluster() throws GenieException {
        final String command1Id = this.commandService.createCommand(
            new Command.Builder(
                "name",
                "user",
                "23.1.0",
                CommandStatus.ACTIVE,
                "pig",
                137324L
            ).build()
        );
        final String command2Id = this.commandService.createCommand(
            new Command.Builder(
                "name2",
                "user2",
                "23.1.1",
                CommandStatus.INACTIVE,
                "pig2",
                23423L
            ).build()
        );
        final List<String> newCommandIds = new ArrayList<>();
        newCommandIds.add(command1Id);
        newCommandIds.add(command2Id);
        Assert.assertEquals(3, this.service.getCommandsForCluster(CLUSTER_1_ID, null).size());
        this.service.updateCommandsForCluster(CLUSTER_1_ID, newCommandIds);
        final List<Command> commands = this.service.getCommandsForCluster(CLUSTER_1_ID, null);
        Assert.assertEquals(2, commands.size());
        Assert.assertEquals(command1Id, commands.get(0).getId());
        Assert.assertEquals(command2Id, commands.get(1).getId());
    }

    /**
     * Test updating commands for the cluster.
     *
     * @throws GenieException For any problem
     */
    @Test(expected = ConstraintViolationException.class)
    public void testUpdateCommandsForClusterNoId() throws GenieException {
        this.service.updateCommandsForCluster(null, new ArrayList<>());
    }

    /**
     * Test updating commands for the cluster.
     *
     * @throws GenieException For any problem
     */
    @Test(expected = ConstraintViolationException.class)
    public void testUpdateCommandsForClusterNoCommands() throws GenieException {
        this.service.updateCommandsForCluster(UUID.randomUUID().toString(), null);
    }

    /**
     * Test removing all commands for the cluster.
     *
     * @throws GenieException For any problem
     */
    @Test
    public void testRemoveAllCommandsForCluster() throws GenieException {
        Assert.assertEquals(3, this.service.getCommandsForCluster(CLUSTER_1_ID, null).size());
        this.service.removeAllCommandsForCluster(CLUSTER_1_ID);
        Assert.assertEquals(0, this.service.getCommandsForCluster(CLUSTER_1_ID, null).size());
    }

    /**
     * Test removing all commands for the cluster.
     *
     * @throws GenieException For any problem
     */
    @Test(expected = ConstraintViolationException.class)
    public void testRemoveAllCommandsForClusterNoId() throws GenieException {
        this.service.removeAllCommandsForCluster(null);
    }

    /**
     * Test removing all commands for the cluster.
     *
     * @throws GenieException For any problem
     */
    @Test
    public void testRemoveCommandForCluster() throws GenieException {
        Assert.assertEquals(3, this.service.getCommandsForCluster(CLUSTER_1_ID, null).size());
        this.service.removeCommandForCluster(CLUSTER_1_ID, COMMAND_1_ID);
        Assert.assertEquals(2, this.service.getCommandsForCluster(CLUSTER_1_ID, null).size());
    }

    /**
     * Test removing all commands for the cluster.
     *
     * @throws GenieException For any problem
     */
    @Test(expected = ConstraintViolationException.class)
    public void testRemoveCommandForClusterNoId() throws GenieException {
        this.service.removeCommandForCluster(null, COMMAND_1_ID);
    }

    /**
     * Test removing all commands for the cluster.
     *
     * @throws GenieException For any problem
     */
    @Test(expected = ConstraintViolationException.class)
    public void testRemoveCommandForClusterNoCmdId() throws GenieException {
        this.service.removeCommandForCluster(CLUSTER_1_ID, null);
    }

    /**
     * Test add tags to cluster.
     *
     * @throws GenieException For any problem
     */
    @Test
    public void testAddTagsToCluster() throws GenieException {
        final String newTag1 = UUID.randomUUID().toString();
        final String newTag2 = UUID.randomUUID().toString();
        final String newTag3 = UUID.randomUUID().toString();

        final Set<String> newTags = new HashSet<>();
        newTags.add(newTag1);
        newTags.add(newTag2);
        newTags.add(newTag3);

        Assert.assertEquals(5, this.service.getTagsForCluster(CLUSTER_1_ID).size());
        this.service.addTagsForCluster(CLUSTER_1_ID, newTags);
        final Set<String> finalTags = this.service.getTagsForCluster(CLUSTER_1_ID);
        Assert.assertEquals(8, finalTags.size());
        Assert.assertTrue(finalTags.contains(newTag1));
        Assert.assertTrue(finalTags.contains(newTag2));
        Assert.assertTrue(finalTags.contains(newTag3));
    }

    /**
     * Test add tags to cluster.
     *
     * @throws GenieException For any problem
     */
    @Test(expected = ConstraintViolationException.class)
    public void testAddTagsToClusterNoId() throws GenieException {
        this.service.addTagsForCluster(null, new HashSet<>());
    }

    /**
     * Test add tags to cluster.
     *
     * @throws GenieException For any problem
     */
    @Test(expected = ConstraintViolationException.class)
    public void testAddTagsToClusterNoTags() throws GenieException {
        this.service.addTagsForCluster(CLUSTER_1_ID, null);
    }

    /**
     * Test update tags for cluster.
     *
     * @throws GenieException For any problem
     */
    @Test
    public void testUpdateTagsForCluster() throws GenieException {
        final String newTag1 = UUID.randomUUID().toString();
        final String newTag2 = UUID.randomUUID().toString();
        final String newTag3 = UUID.randomUUID().toString();

        final Set<String> newTags = new HashSet<>();
        newTags.add(newTag1);
        newTags.add(newTag2);
        newTags.add(newTag3);

        Assert.assertEquals(5, this.service.getTagsForCluster(CLUSTER_1_ID).size());
        this.service.updateTagsForCluster(CLUSTER_1_ID, newTags);
        final Set<String> finalTags = this.service.getTagsForCluster(CLUSTER_1_ID);
        Assert.assertEquals(5, finalTags.size());
        Assert.assertTrue(finalTags.contains(newTag1));
        Assert.assertTrue(finalTags.contains(newTag2));
        Assert.assertTrue(finalTags.contains(newTag3));
    }

    /**
     * Test update tags for cluster.
     *
     * @throws GenieException For any problem
     */
    @Test(expected = ConstraintViolationException.class)
    public void testUpdateTagsForClusterNoId() throws GenieException {
        this.service.updateTagsForCluster(null, new HashSet<>());
    }

    /**
     * Test get tags for cluster.
     *
     * @throws GenieException For any problem
     */
    @Test
    public void testGetTagsForCluster() throws GenieException {
        Assert.assertEquals(5,
            this.service.getTagsForCluster(CLUSTER_1_ID).size());
    }

    /**
     * Test get tags to cluster.
     *
     * @throws GenieException For any problem
     */
    @Test(expected = ConstraintViolationException.class)
    public void testGetTagsForClusterNoId() throws GenieException {
        this.service.getTagsForCluster(null);
    }

    /**
     * Test remove all tags for cluster.
     *
     * @throws GenieException For any problem
     */
    @Test
    public void testRemoveAllTagsForCluster() throws GenieException {
        Assert.assertEquals(5, this.service.getTagsForCluster(CLUSTER_1_ID).size());
        this.service.removeAllTagsForCluster(CLUSTER_1_ID);
        Assert.assertEquals(2, this.service.getTagsForCluster(CLUSTER_1_ID).size());
    }

    /**
     * Test remove all tags for cluster.
     *
     * @throws GenieException For any problem
     */
    @Test(expected = ConstraintViolationException.class)
    public void testRemoveAllTagsForClusterNoId() throws GenieException {
        this.service.removeAllTagsForCluster(null);
    }

    /**
     * Test remove tag for cluster.
     *
     * @throws GenieException For any problem
     */
    @Test
    public void testRemoveTagForCluster() throws GenieException {
        Assert.assertTrue(this.service.getTagsForCluster(CLUSTER_1_ID).contains("prod"));
        this.service.removeTagForCluster(CLUSTER_1_ID, "prod");
        Assert.assertFalse(this.service.getTagsForCluster(CLUSTER_1_ID).contains("prod"));
    }

    /**
     * Test remove tag for cluster.
     *
     * @throws GenieException For any problem
     */
    @Test(expected = ConstraintViolationException.class)
    public void testRemoveTagForClusterNullTag() throws GenieException {
        this.service.removeTagForCluster(CLUSTER_1_ID, null);
    }

    /**
     * Test remove configuration for cluster.
     *
     * @throws GenieException For any problem
     */
    @Test(expected = ConstraintViolationException.class)
    public void testRemoveTagForClusterNoId() throws GenieException {
        this.service.removeTagForCluster(null, "something");
    }
}
